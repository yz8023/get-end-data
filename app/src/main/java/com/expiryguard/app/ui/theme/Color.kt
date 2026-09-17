package com.expiryguard.app.ui.theme

import androidx.compose.ui.graphics.Color

// ========== 主色调 ==========
val Blue500 = Color(0xFF2563EB)
val Blue600 = Color(0xFF1D8CF8)
val Blue700 = Color(0xFF1D4ED8)
val Blue400 = Color(0xFF60A5FA)
val Purple500 = Color(0xFF7C3AED)
val Purple400 = Color(0xFFA78BFA)
val Green500 = Color(0xFF10B981)
val Green400 = Color(0xFF34D399)
val Green600 = Color(0xFF059669)
val Yellow500 = Color(0xFFF59E0B)
val Yellow400 = Color(0xFFFBBF24)
val Red500 = Color(0xFFEF4444)
val Red400 = Color(0xFFF87171)
val Red600 = Color(0xFFDC2626)
val Red700 = Color(0xFFB91C1C)
val Orange500 = Color(0xFFF97316)
val Gray500 = Color(0xFF6B7280)
val Gray400 = Color(0xFF9CA3AF)
val Gray300 = Color(0xFFD1D5DB)
val Gray200 = Color(0xFFE5E7EB)
val Gray100 = Color(0xFFF3F4F6)
val Gray50 = Color(0xFFF9FAFB)

// 浅色主题
val Background = Color(0xFFF5F3F7)
val SurfaceLight = Color(0xFFFFFFFF)
val OnBackgroundLight = Color(0xFF1A1A2E)
val OnSurfaceLight = Color(0xFF1A1A2E)

// 深色主题
val DarkBackground = Color(0xFF0F0F12)
val DarkSurface = Color(0xFF1C1C22)
val OnBackgroundDark = Color(0xFFE5E7EB)
val OnSurfaceDark = Color(0xFFE5E7EB)

// ========== 玻璃拟态（Glassmorphism）配色 ==========
// 玻璃卡片背景
val GlassCardLight = Color(0xE6FFFFFF)
val GlassCardDark = Color(0xCC1C1C22)
// 玻璃边框高光
val GlassBorderLight = Color(0x4DFFFFFF)
val GlassBorderDark = Color(0x33FFFFFF)
// 玻璃内发光（顶部高光反射模拟）
val GlassGlowLight = Color(0x1AFFFFFF)
val GlassGlowDark = Color(0x0DFFFFFF)
// 玻璃阴影
val GlassShadowLight = Color(0x14000000)
val GlassShadowDark = Color(0x40000000)
// 玻璃底部阴影（更深的阴影层）
val GlassShadowDeepLight = Color(0x08000000)
val GlassShadowDeepDark = Color(0x60000000)

// ========== 渐变背景色 ==========
// 浅色模式 - 紫色到蓝色渐变
val GradientStart = Color(0xFFEDE4F3)
val GradientMid = Color(0xFFE4E4F0)
val GradientEnd = Color(0xFFDCE8F5)
// 深色模式
val GradientStartDark = Color(0xFF1A1628)
val GradientMidDark = Color(0xFF161B2E)
val GradientEndDark = Color(0xFF16202E)

// 卡片状态色（带透明度，用于毛玻璃效果）
val CardSafe = Color(0x1A10B981)
val CardExpiring = Color(0x1AF59E0B)
val CardReturnable = Color(0x1A2563EB)
val CardTakeDown = Color(0x1AEF4444)
val CardExpired = Color(0x1AB91C1C)

// 状态流光色
val StatusGlowSafe = Color(0x3310B981)
val StatusGlowExpiring = Color(0x33F59E0B)
val StatusGlowReturnable = Color(0x332563EB)
val StatusGlowTakeDown = Color(0x33EF4444)
val StatusGlowExpired = Color(0x33B91C1C)