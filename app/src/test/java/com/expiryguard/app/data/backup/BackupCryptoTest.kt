package com.expiryguard.app.data.backup

import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupCryptoTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun passwordAndRecoveryCodeBothDecryptTheSamePayload() {
        val plaintext = temporaryFolder.newFile("plain.zip").apply {
            writeBytes(ByteArray(128 * 1024) { (it * 31 + 7).toByte() })
        }
        val encrypted = temporaryFolder.newFile("backup.expiryguard")
        val result = encrypted.outputStream().use { output ->
            BackupCrypto.encrypt(plaintext, output, "correct horse battery".toCharArray())
        }

        encrypted.inputStream().use { assertTrue(BackupCrypto.hasEncryptedHeader(it)) }
        assertFalse(encrypted.readBytes().contentEquals(plaintext.readBytes()))

        val byPassword = temporaryFolder.newFile("by-password.zip")
        encrypted.inputStream().use { input ->
            BackupCrypto.decryptToFile(input, byPassword, "correct horse battery".toCharArray(), null)
        }
        assertArrayEquals(plaintext.readBytes(), byPassword.readBytes())

        val byRecovery = temporaryFolder.newFile("by-recovery.zip")
        val formattedRecoveryCode = result.recoveryCode.chunked(4).joinToString("-")
        encrypted.inputStream().use { input ->
            BackupCrypto.decryptToFile(input, byRecovery, null, formattedRecoveryCode)
        }
        assertArrayEquals(plaintext.readBytes(), byRecovery.readBytes())
    }

    @Test
    fun wrongCredentialAndTamperingAreRejected() {
        val plaintext = temporaryFolder.newFile("plain.zip").apply { writeText("secret backup payload") }
        val encrypted = temporaryFolder.newFile("backup.expiryguard")
        encrypted.outputStream().use { BackupCrypto.encrypt(plaintext, it, "a secure password".toCharArray()) }

        val wrongCredentialOutput = temporaryFolder.newFile("wrong.zip")
        assertBackupFailure {
            encrypted.inputStream().use { input ->
                BackupCrypto.decryptToFile(input, wrongCredentialOutput, "wrong password".toCharArray(), null)
            }
        }
        assertFalse(wrongCredentialOutput.exists())

        val bytes = encrypted.readBytes()
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 0x01).toByte()
        val tampered = temporaryFolder.newFile("tampered.expiryguard").apply { writeBytes(bytes) }
        val tamperedOutput = temporaryFolder.newFile("tampered.zip")
        assertBackupFailure {
            tampered.inputStream().use { input ->
                BackupCrypto.decryptToFile(input, tamperedOutput, "a secure password".toCharArray(), null)
            }
        }
        assertFalse(tamperedOutput.exists())
    }

    @Test
    fun malformedHeaderAndShortPasswordAreRejected() {
        val plaintext = temporaryFolder.newFile("plain.zip").apply { writeText("data") }
        assertBackupFailure {
            File(temporaryFolder.root, "bad").outputStream().use {
                BackupCrypto.encrypt(plaintext, it, "short".toCharArray())
            }
        }
        val malformed = temporaryFolder.newFile("malformed.expiryguard").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        val output = temporaryFolder.newFile("malformed.zip")
        assertBackupFailure {
            malformed.inputStream().use { BackupCrypto.decryptToFile(it, output, null, null) }
        }
    }

    private fun assertBackupFailure(block: () -> Unit) {
        try {
            block()
            fail("Expected BackupException")
        } catch (_: BackupException) {
            // Expected: no unauthenticated plaintext is accepted.
        }
    }
}
