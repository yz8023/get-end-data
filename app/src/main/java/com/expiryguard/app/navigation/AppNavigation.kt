package com.expiryguard.app.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.expiryguard.app.ui.home.HomeScreen
import com.expiryguard.app.ui.product.AddProductScreen
import com.expiryguard.app.ui.product.EditProductScreen
import com.expiryguard.app.ui.product.ProductDetailScreen
import com.expiryguard.app.ui.product.ProductListScreen
import com.expiryguard.app.ui.settings.AboutScreen
import com.expiryguard.app.ui.settings.BackupRestoreScreen
import com.expiryguard.app.ui.settings.CategoryManagementScreen
import com.expiryguard.app.ui.settings.NotificationSettingsScreen
import com.expiryguard.app.ui.settings.SettingsScreen
import com.expiryguard.app.ui.settings.ShelfLifeGroupManagementScreen
import com.expiryguard.app.ui.stats.StatsScreen
import com.expiryguard.app.ui.trash.TrashScreen
import com.expiryguard.app.ui.theme.GlassCardDark
import com.expiryguard.app.ui.theme.GlassCardLight
import com.expiryguard.app.util.ImageUtils
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 路由定义
 */
sealed class Screen(val route: String) {
    /** 首页 */
    data object Home : Screen("home")
    /** 清单列表（基础路由，不含参数） */
    data object ProductList : Screen("product_list")
    /** 添加清单（可选参数 photoPath） */
    data object AddProduct : Screen("add_product?photoPath={photoPath}")
    /** 清单详情 */
    data object ProductDetail : Screen("product_detail/{productId}")
    /** 编辑清单 */
    data object EditProduct : Screen("edit_product/{productId}")
    /** 统计页面 */
    data object Stats : Screen("stats")
    /** 回收站 */
    data object Trash : Screen("trash")
    /** 设置 */
    data object Settings : Screen("settings")
    /** 备份与恢复 */
    data object BackupRestore : Screen("backup_restore")
    /** 分类管理 */
    data object CategoryManagement : Screen("category_management")
    /** 保质期分组管理 */
    data object ShelfLifeGroupManagement : Screen("shelf_life_group_management")
}

/** 清单列表（带搜索和筛选参数的完整路由模式） */
const val PRODUCT_LIST_ROUTE = "product_list?searchQuery={searchQuery}&filter={filter}"

/**
 * 底部导航栏的 Tab 定义
 */
private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

/** 底部导航栏的四个 Tab */
private val bottomNavItems = listOf(
    BottomNavItem("首页", Icons.Default.Home, Screen.Home.route),
    BottomNavItem("清单", Icons.Default.Inventory2, Screen.ProductList.route),
    BottomNavItem("统计", Icons.Default.Assessment, Screen.Stats.route),
    BottomNavItem("设置", Icons.Default.Settings, Screen.Settings.route)
)

/** 需要显示底部导航栏的路由集合 */
private val bottomNavRoutes = bottomNavItems.map { it.route }.toSet()

/**
 * 应用主导航组件
 *
 * 包含 NavHost 路由分发和底部导航栏，使用 Scaffold 作为主框架。
 * 底部导航栏仅在首页、清单列表、统计、设置四个页面可见。
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current

    // 判断当前路由是否需要显示底部导航栏
    // 使用 startsWith 匹配，因为清单列表路由带参数（?searchQuery=...&filter=...）
    val showBottomBar = currentDestination?.route?.let { route ->
        bottomNavRoutes.any { baseRoute -> route == baseRoute || route.startsWith("$baseRoute?") }
    } ?: false

    // ========== 相机相关状态与 Launcher ==========
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraScope = lifecycleOwner.lifecycleScope

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            val uri = cameraImageUri!!
            // 压缩保存在 IO 线程执行，避免阻塞主线程
            cameraScope.launch(Dispatchers.IO) {
                val savedPath = ImageUtils.saveImage(context, uri)
                withContext(Dispatchers.Main) {
                    val route = if (savedPath != null) {
                        "add_product?photoPath=${Uri.encode(savedPath)}"
                    } else {
                        Screen.AddProduct.route
                    }
                    navController.navigate(route)
                }
            }
        } else {
            navController.navigate(Screen.AddProduct.route)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val photoFile = File.createTempFile("photo_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", photoFile
            )
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
            navController.navigate(Screen.AddProduct.route)
        }
    }

    fun launchCamera() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPermission) {
            val photoFile = File.createTempFile("photo_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", photoFile
            )
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                val bg = MaterialTheme.colorScheme.background
                val isDarkBg = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue < 0.5f
                val navBarColor = if (isDarkBg) GlassCardDark else GlassCardLight
                NavigationBar(
                    containerColor = navBarColor
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.route?.let { route ->
                            item.route == route || route.startsWith("${item.route}?")
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) Color.Unspecified
                                    else Color.Gray.copy(alpha = 0.6f)
                                )
                            },
                            label = { Text(text = item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ==================== 首页 ====================
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToAddProduct = {
                        launchCamera()
                    },
                    onNavigateToProductDetail = { productId ->
                        navController.navigate("product_detail/$productId")
                    },
                    onNavigateToProductList = { searchQuery, filter ->
                        val encodedQuery = if (searchQuery.isNotBlank()) {
                            URLEncoder.encode(searchQuery, "UTF-8")
                        } else {
                            ""
                        }
                        val route = buildString {
                            append("product_list?")
                            if (encodedQuery.isNotBlank()) {
                                append("searchQuery=$encodedQuery&")
                            }
                            append("filter=$filter")
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    viewModel = hiltViewModel()
                )
            }

            // ==================== 清单列表 ====================
            composable(
                route = PRODUCT_LIST_ROUTE,
                arguments = listOf(
                    navArgument("searchQuery") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                    navArgument("filter") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val rawQuery = backStackEntry.arguments?.getString("searchQuery") ?: ""
                val searchQuery = if (rawQuery.isNotBlank()) {
                    URLDecoder.decode(rawQuery, "UTF-8")
                } else {
                    ""
                }
                val rawFilter = backStackEntry.arguments?.getString("filter") ?: ""
                ProductListScreen(
                    initialSearchQuery = searchQuery,
                    initialFilter = rawFilter,
                    onNavigateToAddProduct = {
                        launchCamera()
                    },
                    onNavigateToProductDetail = { productId ->
                        navController.navigate("product_detail/$productId")
                    },
                    onNavigateToEditProduct = { productId ->
                        navController.navigate("edit_product/$productId")
                    },
                    onNavigateToTrash = {
                        navController.navigate(Screen.Trash.route)
                    },
                    onNavigateToCategoryManagement = {
                        navController.navigate(Screen.CategoryManagement.route)
                    },
                    viewModel = hiltViewModel()
                )
            }

            // ==================== 添加清单 ====================
            composable(
                route = Screen.AddProduct.route,
                arguments = listOf(
                    navArgument("photoPath") {
                        type = NavType.StringType
                        defaultValue = null
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val photoPath = backStackEntry.arguments?.getString("photoPath")
                AddProductScreen(
                    photoPath = photoPath,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProductDetail = { productId ->
                        navController.navigate("product_detail/$productId") {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToGroupManagement = {
                        navController.navigate(Screen.ShelfLifeGroupManagement.route)
                    },
                    viewModel = hiltViewModel()
                )
            }

            // ==================== 清单详情 ====================
            composable(
                route = Screen.ProductDetail.route,
                arguments = listOf(
                    navArgument("productId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId")
                    ?: return@composable
                ProductDetailScreen(
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() },
                    onEdit = { id ->
                        navController.navigate("edit_product/$id")
                    },
                    viewModel = hiltViewModel()
                )
            }

            // ==================== 编辑清单 ====================
            composable(
                route = Screen.EditProduct.route,
                arguments = listOf(
                    navArgument("productId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId")
                    ?: return@composable
                EditProductScreen(
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = hiltViewModel()
                )
            }

            // ==================== 统计页面 ====================
            composable(Screen.Stats.route) {
                StatsScreen(
                    onNavigate = { productIdStr ->
                        navController.navigate("product_detail/$productIdStr")
                    },
                    viewModel = hiltViewModel()
                )
            }

            // ==================== 回收站 ====================
            composable(Screen.Trash.route) {
                TrashScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = hiltViewModel()
                )
            }

            // ==================== 设置 ====================
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigate = { route ->
                        when (route) {
                            "backup_restore" -> navController.navigate(Screen.BackupRestore.route)
                            "category_management" -> navController.navigate(Screen.CategoryManagement.route)
                            "shelf_life_group_management" -> navController.navigate(Screen.ShelfLifeGroupManagement.route)
                            "notification_settings" -> { navController.navigate("notification_settings") }
                            "about" -> { navController.navigate("about") }
                        }
                    },
                    viewModel = hiltViewModel()
                )
            }

            // ==================== 备份与恢复 ====================
            composable(Screen.BackupRestore.route) {
                BackupRestoreScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = hiltViewModel()
                )
            }

            // ==================== 分类管理 ====================
            composable(Screen.CategoryManagement.route) {
                CategoryManagementScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = hiltViewModel()
                )
            }

            // ==================== 保质期分组管理 ====================
            composable(Screen.ShelfLifeGroupManagement.route) {
                ShelfLifeGroupManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ==================== 通知设置 ====================
            composable("notification_settings") {
                NotificationSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = hiltViewModel()
                )
            }

            // ==================== 关于页面 ====================
            composable("about") {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}