package com.expiryguard.app.data.backup

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Portable, authenticated encryption for exported backups.
 *
 * A fresh 256-bit data key is generated for every backup. The ZIP payload is
 * encrypted with AES-GCM; that data key is wrapped twice: once with a key
 * derived from the user's password and once with a high-entropy recovery code.
 * Either credential can therefore restore the backup on another device. No
 * password, recovery code, or plaintext key is persisted by this class.
 *
 * Container layout (all integers are big endian):
 *   8 bytes magic | 4 bytes metadata length | metadata JSON | ciphertext+tag
 * The metadata is authenticated as GCM AAD, so changing KDF or wrapping
 * parameters also fails authentication.
 */
object BackupCrypto {
    private val MAGIC = "EGUARD2\n".toByteArray(StandardCharsets.US_ASCII)
    private const val MAX_METADATA_BYTES = 16 * 1024
    private const val MAX_PASSWORD_CHARS = 256
    private const val MIN_PASSWORD_CHARS = 8
    private const val KDF_ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val DATA_KEY_BYTES = 32
    private const val RECOVERY_BYTES = 16
    private const val MAX_PLAINTEXT_BYTES = 1100L * 1024L * 1024L
    private val gson = Gson()
    private val random = SecureRandom()

    data class EncryptionResult(val recoveryCode: String)

    private data class Metadata(
        val format: String = FORMAT,
        val version: Int = VERSION,
        val kdf: String = KDF,
        val iterations: Int = KDF_ITERATIONS,
        @SerializedName("password_salt") val passwordSalt: String,
        @SerializedName("password_wrap_iv") val passwordWrapIv: String,
        @SerializedName("password_wrapped_key") val passwordWrappedKey: String,
        @SerializedName("recovery_salt") val recoverySalt: String,
        @SerializedName("recovery_wrap_iv") val recoveryWrapIv: String,
        @SerializedName("recovery_wrapped_key") val recoveryWrappedKey: String,
        @SerializedName("payload_iv") val payloadIv: String
    )

    /** Reads only the fixed magic; the caller must close the supplied stream. */
    fun hasEncryptedHeader(input: InputStream): Boolean {
        val actual = ByteArray(MAGIC.size)
        var offset = 0
        while (offset < actual.size) {
            val read = input.read(actual, offset, actual.size - offset)
            if (read < 0) return false
            offset += read
        }
        return actual.contentEquals(MAGIC)
    }

    /**
     * Encrypts [source] into [destination]. The returned recovery code is
     * shown once by the UI; it is never written to logs or shared preferences.
     */
    fun encrypt(source: File, destination: OutputStream, password: CharArray): EncryptionResult {
        validatePassword(password)
        val dataKey = ByteArray(DATA_KEY_BYTES).also(random::nextBytes)
        val passwordSalt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val recoverySalt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val passwordWrapIv = ByteArray(IV_BYTES).also(random::nextBytes)
        val recoveryWrapIv = ByteArray(IV_BYTES).also(random::nextBytes)
        val payloadIv = ByteArray(IV_BYTES).also(random::nextBytes)
        val recoveryCode = newRecoveryCode()
        var passwordSpec: PBEKeySpec? = null
        var recoverySpec: PBEKeySpec? = null
        try {
            passwordSpec = PBEKeySpec(password, passwordSalt, KDF_ITERATIONS, KEY_BITS)
            recoverySpec = PBEKeySpec(
                recoveryCode.toCharArray(), recoverySalt, KDF_ITERATIONS, KEY_BITS
            )
            val passwordKey = derive(passwordSpec)
            val recoveryKey = derive(recoverySpec)
            val metadata = Metadata(
                passwordSalt = b64(passwordSalt),
                passwordWrapIv = b64(passwordWrapIv),
                passwordWrappedKey = b64(wrap(dataKey, passwordKey, passwordWrapIv, PASSWORD_AAD)),
                recoverySalt = b64(recoverySalt),
                recoveryWrapIv = b64(recoveryWrapIv),
                recoveryWrappedKey = b64(wrap(dataKey, recoveryKey, recoveryWrapIv, RECOVERY_AAD)),
                payloadIv = b64(payloadIv)
            )
            val metadataBytes = gson.toJson(metadata).toByteArray(StandardCharsets.UTF_8)
            require(metadataBytes.size <= MAX_METADATA_BYTES)
            destination.write(MAGIC)
            destination.write(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(metadataBytes.size).array())
            destination.write(metadataBytes)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(dataKey, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, payloadIv)
            )
            cipher.updateAAD(metadataBytes)
            source.inputStream().buffered().use { input ->
                copyThroughCipher(input, destination, cipher, Long.MAX_VALUE)
            }
            destination.flush()
            return EncryptionResult(recoveryCode)
        } catch (e: GeneralSecurityException) {
            throw BackupException("无法初始化备份加密", e)
        } finally {
            passwordSpec?.clearPassword()
            recoverySpec?.clearPassword()
            dataKey.fill(0)
        }
    }

    /** Decrypts and authenticates the complete payload before returning. */
    fun decryptToFile(
        input: InputStream,
        destination: File,
        password: CharArray?,
        recoveryCode: String?
    ) {
        val magic = ByteArray(MAGIC.size)
        readFully(input, magic)
        if (!magic.contentEquals(MAGIC)) throw BackupException("不是受支持的加密备份")
        val lengthBytes = ByteArray(Int.SIZE_BYTES)
        readFully(input, lengthBytes)
        val metadataLength = ByteBuffer.wrap(lengthBytes).int
        if (metadataLength <= 0 || metadataLength > MAX_METADATA_BYTES) {
            throw BackupException("加密备份头无效")
        }
        val metadataBytes = ByteArray(metadataLength)
        readFully(input, metadataBytes)
        val metadata = try {
            gson.fromJson(metadataBytes.toString(StandardCharsets.UTF_8), Metadata::class.java)
        } catch (e: Exception) {
            throw BackupException("加密备份头格式无效", e)
        } ?: throw BackupException("加密备份头为空")
        validateMetadata(metadata)

        var spec: PBEKeySpec? = null
        try {
            val usingPassword = password != null && password.isNotEmpty()
            if (!usingPassword && recoveryCode.isNullOrBlank()) {
                throw BackupException("该备份已加密，请输入备份密码或恢复密钥")
            }
            val salt: ByteArray
            val wrapIv: ByteArray
            val wrappedKey: ByteArray
            val aad: ByteArray
            if (usingPassword) {
                validatePassword(password)
                salt = decode(metadata.passwordSalt, SALT_BYTES)
                wrapIv = decode(metadata.passwordWrapIv, IV_BYTES)
                wrappedKey = decode(metadata.passwordWrappedKey, DATA_KEY_BYTES + GCM_TAG_BITS / 8)
                aad = PASSWORD_AAD
                spec = PBEKeySpec(password, salt, metadata.iterations, KEY_BITS)
            } else {
                val normalized = normalizeRecoveryCode(recoveryCode!!)
                salt = decode(metadata.recoverySalt, SALT_BYTES)
                wrapIv = decode(metadata.recoveryWrapIv, IV_BYTES)
                wrappedKey = decode(metadata.recoveryWrappedKey, DATA_KEY_BYTES + GCM_TAG_BITS / 8)
                aad = RECOVERY_AAD
                spec = PBEKeySpec(normalized.toCharArray(), salt, metadata.iterations, KEY_BITS)
            }
            val wrappingKey = derive(spec)
            val dataKey = try {
                unwrap(wrappedKey, wrappingKey, wrapIv, aad)
            } catch (e: GeneralSecurityException) {
                throw BackupException("密码或恢复密钥不正确，或备份已损坏", e)
            }
            try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(dataKey, "AES"),
                    GCMParameterSpec(GCM_TAG_BITS, decode(metadata.payloadIv, IV_BYTES))
                )
                cipher.updateAAD(metadataBytes)
                destination.outputStream().buffered().use { output ->
                    try {
                        copyThroughCipher(input, output, cipher, MAX_PLAINTEXT_BYTES)
                    } catch (e: GeneralSecurityException) {
                        throw BackupException("密码或恢复密钥不正确，或备份已损坏", e)
                    }
                }
            } finally {
                dataKey.fill(0)
            }
        } catch (e: BackupException) {
            destination.delete()
            throw e
        } catch (e: GeneralSecurityException) {
            destination.delete()
            throw BackupException("备份解密失败", e)
        } catch (e: Exception) {
            destination.delete()
            if (e is BackupException) throw e
            throw BackupException("备份解密失败", e)
        } finally {
            spec?.clearPassword()
        }
    }

    fun normalizeRecoveryCode(value: String): String {
        val normalized = value.filter { it.isLetterOrDigit() }.uppercase()
        if (normalized.length != RECOVERY_BYTES * 2 || normalized.any { it !in '0'..'9' && it !in 'A'..'F' }) {
            throw BackupException("恢复密钥格式无效")
        }
        return normalized
    }

    private fun validatePassword(password: CharArray) {
        if (password.size < MIN_PASSWORD_CHARS || password.size > MAX_PASSWORD_CHARS) {
            throw BackupException("备份密码长度应为 $MIN_PASSWORD_CHARS–$MAX_PASSWORD_CHARS 个字符")
        }
    }

    private fun validateMetadata(metadata: Metadata) {
        if (metadata.format != FORMAT || metadata.version != VERSION ||
            metadata.kdf != KDF || metadata.iterations !in 100_000..2_000_000
        ) {
            throw BackupException("不支持的加密备份参数")
        }
        // Decode and size-check all fields before doing expensive KDF work.
        decode(metadata.passwordSalt, SALT_BYTES)
        decode(metadata.passwordWrapIv, IV_BYTES)
        decode(metadata.passwordWrappedKey, DATA_KEY_BYTES + GCM_TAG_BITS / 8)
        decode(metadata.recoverySalt, SALT_BYTES)
        decode(metadata.recoveryWrapIv, IV_BYTES)
        decode(metadata.recoveryWrappedKey, DATA_KEY_BYTES + GCM_TAG_BITS / 8)
        decode(metadata.payloadIv, IV_BYTES)
    }

    private fun derive(spec: PBEKeySpec): SecretKeySpec {
        val encoded = SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
        return SecretKeySpec(encoded, "AES")
    }

    private fun wrap(key: ByteArray, wrappingKey: SecretKeySpec, iv: ByteArray, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(aad)
        return cipher.doFinal(key)
    }

    private fun unwrap(wrapped: ByteArray, wrappingKey: SecretKeySpec, iv: ByteArray, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(aad)
        return cipher.doFinal(wrapped)
    }

    private fun copyThroughCipher(
        input: InputStream,
        output: OutputStream,
        cipher: Cipher,
        maxBytes: Long
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) throw BackupException("备份内容超过安全大小限制")
            val transformed = cipher.update(buffer, 0, read)
            if (transformed != null) output.write(transformed)
        }
        val transformed = cipher.doFinal()
        if (transformed != null) output.write(transformed)
    }

    private fun newRecoveryCode(): String {
        val bytes = ByteArray(RECOVERY_BYTES).also(random::nextBytes)
        return bytes.joinToString("") { "%02X".format(it) }
    }

    private fun b64(value: ByteArray): String = Base64.getEncoder().encodeToString(value)

    private fun decode(value: String, expectedBytes: Int): ByteArray {
        val decoded = try {
            Base64.getDecoder().decode(value)
        } catch (e: IllegalArgumentException) {
            throw BackupException("加密备份头字段无效", e)
        }
        if (decoded.size != expectedBytes) throw BackupException("加密备份头字段长度无效")
        return decoded
    }

    private fun readFully(input: InputStream, target: ByteArray) {
        var offset = 0
        while (offset < target.size) {
            val read = input.read(target, offset, target.size - offset)
            if (read < 0) throw BackupException("加密备份头不完整")
            offset += read
        }
    }

    private const val FORMAT = "expiryguard-encrypted-backup"
    private const val VERSION = 1
    private const val KDF = "PBKDF2WithHmacSHA256"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private val PASSWORD_AAD = "ExpiryGuard/password-key/v1".toByteArray(StandardCharsets.UTF_8)
    private val RECOVERY_AAD = "ExpiryGuard/recovery-key/v1".toByteArray(StandardCharsets.UTF_8)
}
