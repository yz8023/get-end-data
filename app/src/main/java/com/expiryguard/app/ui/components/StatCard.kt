package com.expiryguard.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expiryguard.app.ui.liquid.components.liquidGlass
import com.expiryguard.app.ui.liquid.theme.AppShape

/**
 * 液态玻璃风格统计卡片，显示大号数字和标签文字。
 *
 * 内部使用液态玻璃面板 + 大号强调色数字 + 弱化标签，
 * 与设计系统保持一致的玻璃卡片语言。
 *
 * @param label 统计项标签
 * @param value 统计数值
 * @param color 主题色
 * @param modifier 修饰符（可选），用于自定义布局尺寸
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .liquidGlass(
                shape = AppShape.card,
                tint = color.copy(alpha = 0.10f),
                elevation = 1.dp,
                borderAlpha = 0.32f
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
