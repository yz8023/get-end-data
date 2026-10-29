package com.expiryguard.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** 图片只保存在应用私有 product_images 目录，路径操作均限制在该目录内。 */
object ImageUtils {
    private const val IMAGE_DIR = "product_images"

    private fun getImageDir(context: Context): File = File(context.filesDir, IMAGE_DIR).apply {
        if (!exists() && !mkdirs()) throw IllegalStateException("无法创建图片目录")
    }

    private fun getCameraDir(context: Context): File = File(context.cacheDir, "camera").apply {
        if (!exists() && !mkdirs()) throw IllegalStateException("无法创建相机临时目录")
    }

    /** 相机临时原图只落在 FileProvider 明确开放的 camera/ 子目录。 */
    fun createCameraImageFile(context: Context): File =
        File.createTempFile("photo_", ".jpg", getCameraDir(context))

    private fun isManagedImage(context: Context, file: File): Boolean = runCatching {
        val root = File(context.filesDir, IMAGE_DIR).canonicalFile
        val candidate = file.canonicalFile
        candidate.path.startsWith(root.path + File.separator)
    }.getOrDefault(false)

    /** 采样解码、按 EXIF 方向纠正后保存 JPEG，避免大图内存峰值和旋转照片。 */
    fun saveImage(context: Context, uri: Uri, maxSize: Int = 1080): String? {
        var bitmap: Bitmap? = null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            } ?: return null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val targetSize = maxSize.coerceAtLeast(1)
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > targetSize ||
                bounds.outHeight / sampleSize > targetSize
            ) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return null
            bitmap = decoded
            val oriented = applyExifOrientation(context, uri, decoded)
            if (oriented !== decoded) {
                decoded.recycle()
                bitmap = oriented
            }
            saveBitmap(context, oriented)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            bitmap?.recycle()
        }
    }

    private fun applyExifOrientation(context: Context, uri: Uri, source: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun saveBitmap(context: Context, bitmap: Bitmap): String? {
        val file = try {
            File(getImageDir(context), "${UUID.randomUUID()}.jpg")
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return try {
            val compressed = FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            if (!compressed || file.length() == 0L) {
                file.delete()
                null
            } else {
                file.absolutePath
            }
        } catch (e: Exception) {
            file.delete()
            e.printStackTrace()
            null
        }
    }

    fun getImageFile(context: Context, path: String): File? {
        val file = File(path)
        return if (isManagedImage(context, file) && file.isFile) file else null
    }

    /** 不允许利用数据库中的路径删除应用沙箱内的其它文件。 */
    fun deleteImage(context: Context, path: String): Boolean = try {
        val file = File(path)
        isManagedImage(context, file) && file.delete()
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    /** 相机回调结束后删除临时原图；只允许删除应用 cache 根下本应用创建的 photo_ 文件。 */
    fun deleteTemporaryCameraImage(context: Context, file: File) {
        runCatching {
            val cache = context.cacheDir.canonicalFile
            val camera = File(cache, "camera").canonicalFile
            val candidate = file.canonicalFile
            if ((candidate.parentFile == camera || candidate.parentFile == cache) &&
                candidate.name.startsWith("photo_")
            ) {
                candidate.delete()
            }
        }
    }

    fun cleanupStaleCameraImages(context: Context) {
        val cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        val roots = listOf(context.cacheDir, File(context.cacheDir, "camera"))
        roots.flatMap { it.listFiles()?.toList().orEmpty() }.forEach { file ->
            if (file.isFile && file.name.startsWith("photo_") && file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    fun compressImage(context: Context, uri: Uri, maxSize: Int = 1080): String? =
        saveImage(context, uri, maxSize)
}
