package com.expiryguard.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 图片工具类，提供图片保存、读取、删除和压缩功能。
 */
object ImageUtils {

    private const val IMAGE_DIR = "product_images"

    /**
     * 获取图片存储目录
     */
    private fun getImageDir(context: Context): File {
        val dir = File(context.filesDir, IMAGE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 从 Uri 保存图片到应用内部存储（自动压缩到 maxSize 以内）
     *
     * 压缩逻辑：
     * 1. 读取原图尺寸（inJustDecodeBounds，不加载像素）
     * 2. 计算 2 的幂采样率，使宽高缩小到 maxSize 以内
     * 3. 按采样率解码后以 JPEG 90 质量保存
     *
     * 相比直接复制原图，可大幅降低磁盘占用与解码内存，减少列表卡顿。
     *
     * @return 保存后的图片绝对路径，失败返回 null
     */
    fun saveImage(context: Context, uri: Uri, maxSize: Int = 1080): String? {
        return try {
            // 第一步：读取原图尺寸信息（不加载像素）
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val outWidth = options.outWidth
            val outHeight = options.outHeight
            if (outWidth <= 0 || outHeight <= 0) return null

            // 计算采样率，使图片缩小到 maxSize 以内
            var sampleSize = 1
            while (outWidth / sampleSize > maxSize || outHeight / sampleSize > maxSize) {
                sampleSize *= 2
            }

            // 第二步：按采样率解码
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val inputStream2 = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2.close()
            if (bitmap == null) return null

            // 第三步：保存压缩后的图片
            val fileName = "${UUID.randomUUID()}.jpg"
            val file = File(getImageDir(context), fileName)
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            bitmap.recycle()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 保存 Bitmap 到应用内部存储
     *
     * @return 保存后的图片绝对路径，失败返回 null
     */
    fun saveBitmap(context: Context, bitmap: Bitmap): String? {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val file = File(getImageDir(context), fileName)
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 根据路径获取图片文件
     */
    fun getImageFile(context: Context, path: String): File? {
        val file = File(path)
        return if (file.exists()) file else null
    }

    /**
     * 删除指定路径的图片文件
     */
    fun deleteImage(context: Context, path: String): Boolean {
        return try {
            File(path).delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 压缩图片到指定最大尺寸（宽高最大值），保持宽高比。
     *
     * @param uri 原始图片 Uri
     * @param maxSize 最大宽高值，默认 1080px
     * @return 压缩后保存的图片路径，失败返回 null
     */
    fun compressImage(context: Context, uri: Uri, maxSize: Int = 1080): String? {
        return try {
            // 第一步：读取原图尺寸信息
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val outWidth = options.outWidth
            val outHeight = options.outHeight

            // 计算采样率，使图片缩小到 maxSize 以内
            var sampleSize = 1
            while (outWidth / sampleSize > maxSize || outHeight / sampleSize > maxSize) {
                sampleSize *= 2
            }

            // 第二步：按采样率解码
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val inputStream2 = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2.close()

            // 第三步：保存压缩后的图片
            bitmap?.let { saveBitmap(context, it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}