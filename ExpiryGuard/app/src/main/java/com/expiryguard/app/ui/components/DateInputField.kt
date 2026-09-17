package com.expiryguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expiryguard.app.ui.theme.Gray500
import com.expiryguard.app.ui.theme.Green500
import com.expiryguard.app.ui.theme.Red500
import com.expiryguard.app.util.DateUtils
import java.time.LocalDate

/**
 * 日期输入组件 —— 支持「文本输入」与「日历选择」两种方式。
 *
 * 文本输入支持多种格式自动解析，例如：
 * - 2019.1.1
 * - 2019年1月1日
 * - 2019 1 1
 * - 2019年1.1
 *
 * @param label 字段标签（如 "到期日期" / "生产日期"）
 * @param hint 输入框提示文字
 * @param value 当前选中日期（时间戳，可空）
 * @param accentColor 主题色，用于图标/选中态，便于区分生产日期与到期日期
 * @param onDateSelected 选中日期回调（日历或解析成功后触发）
 * @param onDateCleared 清空日期回调（可空）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateInputField(
    label: String,
    hint: String,
    value: Long?,
    accentColor: Color,
    onDateSelected: (LocalDate) -> Unit,
    onDateCleared: (() -> Unit)? = null
) {
    var textInput by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf(false) }
    var hasTyped by remember { mutableStateOf(false) }

    // 外部日期变化时同步文本（如保质期自动计算到期日）
    LaunchedEffect(value) {
        if (value != null && !hasTyped) {
            textInput = DateUtils.formatDate(value)
        }
    }

    val isParsed = remember(textInput) {
        if (textInput.isBlank()) null
        else DateUtils.parseDate(textInput)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showPicker = true },
        selected = value != null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行：带色点的标签 + 日历选择按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(accentColor, RoundedCornerShape(5.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (value != null) accentColor else Gray500,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showPicker = true }) {
                    Text(if (value != null) "修改" else "选择")
                }
                IconButton(onClick = { showPicker = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "打开日历",
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 文本输入框：支持多格式输入
            OutlinedTextField(
                value = textInput,
                onValueChange = { input ->
                    textInput = input
                    hasTyped = true
                    if (input.isBlank()) {
                        parseError = false
                    } else {
                        parseError = DateUtils.parseDate(input) == null
                    }
                    // 输入即识别：解析成功立刻回写
                    if (parseError) {
                        // 不自动清除，等待用户修正
                    } else {
                        DateUtils.parseDate(input)?.let { parsed ->
                            onDateSelected(parsed)
                        }
                    }
                },
                placeholder = { Text(hint) },
                label = {
                    Text(
                        text = if (value != null) {
                            "已选：${DateUtils.formatDate(value)}"
                        } else {
                            hint
                        }
                    )
                },
                isError = parseError,
                supportingText = {
                    if (parseError) {
                        Text("格式无法识别，示例：2019.1.1 / 2019年1月1日 / 2019 1 1")
                    } else if (isParsed != null) {
                        Text("已识别：${isParsed.year}年${isParsed.monthValue}月${isParsed.dayOfMonth}日", color = Green500)
                    } else {
                        Text("可直接输入日期，如 2019.1.1 或 2019年1月1日")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )

            // 已选日期状态行
            if (value != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Green500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "已确认：${DateUtils.formatDate(value)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Green500,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // 日历选择对话框
    if (showPicker) {
        DatePickerDialog(
            initialDate = value?.let { DateUtils.toLocalDate(it) } ?: LocalDate.now(),
            onDateSelected = { date ->
                onDateSelected(date)
                textInput = DateUtils.formatDate(DateUtils.toTimestamp(date))
                hasTyped = false
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}
