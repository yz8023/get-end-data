package com.expiryguard.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.expiryguard.app.ui.liquid.components.liquidGlass

/**
 * 液态玻璃顶栏。
 *
 * 与 Material3 [TopAppBar] 参数兼容（额外接收并忽略 `colors`，让既有调用点零改动迁移），
 * 容器强制透明，底层叠加真实 backdrop 折射的玻璃层，仅保留下方圆角。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    colors: Any? = null
) {
    TopAppBar(
        title = title,
        modifier = modifier.liquidGlass(
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            elevation = 2.dp,
            borderAlpha = 0.35f
        ),
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
