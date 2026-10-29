package com.expiryguard.app.data.export

import android.content.Context
import android.net.Uri
import com.expiryguard.app.data.repository.ProductRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import javax.inject.Inject

/** 流式导出到用户通过 SAF 选择的 Uri，不生成难以取走的私有临时文件。 */
class DataExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ProductRepository
) {
    private val gson = Gson()

    suspend fun exportCsv(uri: Uri): Int = withContext(Dispatchers.IO) {
        val products = repository.getAllActiveProducts().first()
        openWriter(uri).use { writer ->
            writer.write("名称,条形码,到期日期,保质期(天),备注,创建时间\n")
            products.forEach { product ->
                val row = listOf(
                    product.name,
                    product.barcode.orEmpty(),
                    product.expiryDate.toString(),
                    product.shelfLifeDays.toString(),
                    product.notes.orEmpty(),
                    product.createdAt.toString()
                )
                writer.write(row.joinToString(",") { csvEscape(it) })
                writer.write("\n")
            }
        }
        products.size
    }

    suspend fun exportJson(uri: Uri): Int = withContext(Dispatchers.IO) {
        val products = repository.getAllActiveProducts().first()
        openWriter(uri).use { writer -> gson.toJson(products, writer) }
        products.size
    }

    private fun openWriter(uri: Uri): BufferedWriter {
        val output = context.contentResolver.openOutputStream(uri, "w")
            ?: throw IllegalStateException("无法写入所选导出位置")
        return BufferedWriter(OutputStreamWriter(output, Charsets.UTF_8))
    }

    private fun csvEscape(value: String): String {
        val safeValue = if (value.isNotEmpty() && value[0] in setOf('=', '+', '-', '@', '\t', '\r')) {
            "'$value"
        } else {
            value
        }
        return "\"${safeValue.replace("\"", "\"\"")}\""
    }
}
