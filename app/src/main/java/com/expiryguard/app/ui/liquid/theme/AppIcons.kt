package com.expiryguard.app.ui.liquid.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Bundled icon set.
 *
 * Only the nine icons the template actually uses are vendored here, as plain
 * [ImageVector]s, instead of pulling the whole `material-icons-extended`
 * module (≈2000 icons, the single largest contributor to debug APK size).
 * Path data: Apache-2.0, Copyright 2024 The Android Open Source Project,
 * as published in androidx material icons.
 */
object AppIcons {
    val Animation: ImageVector by lazy {
            ImageVector.Builder(
                name = "AppIcons.Animation",
                defaultWidth = 24.dp, defaultHeight = 24.dp,
                viewportWidth = 24f, viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                            moveTo(15.0f, 2.0f)
                            curveToRelative(-2.71f, 0.0f, -5.05f, 1.54f, -6.22f, 3.78f)
                            curveToRelative(-1.28f, 0.67f, -2.34f, 1.72f, -3.0f, 3.0f)
                            curveTo(3.54f, 9.95f, 2.0f, 12.29f, 2.0f, 15.0f)
                            curveToRelative(0.0f, 3.87f, 3.13f, 7.0f, 7.0f, 7.0f)
                            curveToRelative(2.71f, 0.0f, 5.05f, -1.54f, 6.22f, -3.78f)
                            curveToRelative(1.28f, -0.67f, 2.34f, -1.72f, 3.0f, -3.0f)
                            curveTo(20.46f, 14.05f, 22.0f, 11.71f, 22.0f, 9.0f)
                            curveTo(22.0f, 5.13f, 18.87f, 2.0f, 15.0f, 2.0f)
                            close()
                            moveTo(9.0f, 20.0f)
                            curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
                            curveToRelative(0.0f, -1.12f, 0.37f, -2.16f, 1.0f, -3.0f)
                            curveToRelative(0.0f, 3.87f, 3.13f, 7.0f, 7.0f, 7.0f)
                            curveTo(11.16f, 19.63f, 10.12f, 20.0f, 9.0f, 20.0f)
                            close()
                            moveTo(12.0f, 17.0f)
                            curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
                            curveToRelative(0.0f, -1.12f, 0.37f, -2.16f, 1.0f, -3.0f)
                            curveToRelative(0.0f, 3.86f, 3.13f, 6.99f, 7.0f, 7.0f)
                            curveTo(14.16f, 16.63f, 13.12f, 17.0f, 12.0f, 17.0f)
                            close()
                            moveTo(16.7f, 13.7f)
                            curveTo(16.17f, 13.89f, 15.6f, 14.0f, 15.0f, 14.0f)
                            curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
                            curveToRelative(0.0f, -0.6f, 0.11f, -1.17f, 0.3f, -1.7f)
                            curveTo(10.83f, 7.11f, 11.4f, 7.0f, 12.0f, 7.0f)
                            curveToRelative(2.76f, 0.0f, 5.0f, 2.24f, 5.0f, 5.0f)
                            curveTo(17.0f, 12.6f, 16.89f, 13.17f, 16.7f, 13.7f)
                            close()
                            moveTo(19.0f, 12.0f)
                            curveToRelative(0.0f, -3.86f, -3.13f, -6.99f, -7.0f, -7.0f)
                            curveToRelative(0.84f, -0.63f, 1.87f, -1.0f, 3.0f, -1.0f)
                            curveToRelative(2.76f, 0.0f, 5.0f, 2.24f, 5.0f, 5.0f)
                            curveTo(20.0f, 10.12f, 19.63f, 11.16f, 19.0f, 12.0f)
                            close()   }
            }.build()
    }

    val BlurOn: ImageVector by lazy {
            ImageVector.Builder(
                name = "AppIcons.BlurOn",
                defaultWidth = 24.dp, defaultHeight = 24.dp,
                viewportWidth = 24f, viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                            moveTo(6.0f, 13.0f)
                            curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                            reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                            reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                            reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                            close()
                            moveTo(6.0f, 17.0f)
                            curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                            reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                            reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                            reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                            close()
                            moveTo(6.0f, 9.0f)
                            curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                            reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                            reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                            reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                            close()
                            moveTo(3.0f, 9.5f)
                            curveToRelative(-0.28f, 0.0f, -0.5f, 0.22f, -0.5f, 0.5f)
                            reflectiveCurveToRelative(0.22f, 0.5f, 0.5f, 0.5f)
                            reflectiveCurveToRelative(0.5f, -0.22f, 0.5f, -0.5f)
                            reflectiveCurveToRelative(-0.22f, -0.5f, -0.5f, -0.5f)
                            close()
                            moveTo(6.0f, 5.0f)
                            curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                            reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                            reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                            reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                            close()
                            moveTo(21.0f, 10.5f)
                            curveToRelative(0.28f, 0.0f, 0.5f, -0.22f, 0.5f, -0.5f)
                            reflectiveCurveToRelative(-0.22f, -0.5f, -0.5f, -0.5f)
                            reflectiveCurveToRelative(-0.5f, 0.22f, -0.5f, 0.5f)
                            reflectiveCurveToRelative(0.22f, 0.5f, 0.5f, 0.5f)
                            close()
                            moveTo(14.0f, 7.0f)
                            curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                            reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                            reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                            reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                            close()
                            moveTo(14.0f, 3.5f)
                            curveToRelative(0.28f, 0.0f, 0.5f, -0.22f, 0.5f, -0.5f)
                            reflectiveCurveToRelative(-0.22f, -0.5f, -0.5f, -0.5f)
                            reflectiveCurveToRelative(-0.5f, 0.22f, -0.5f, 0.5f)
                            reflectiveCurveToRelative(0.22f, 0.5f, 0.5f, 0.5f)
                            close()
                            moveTo(3.0f, 13.5f)
                            curveToRelative(-0.28f, 0.0f, -0.5f, 0.22f, -0.5f, 0.5f)
                            reflectiveCurveToRelative(0.22f, 0.5f, 0.5f, 0.5f)
                            reflectiveCurveToRelative(0.5f, -0.22f, 0.5f, -0.5f)
                            reflectiveCurveToRelative(-0.22f, -0.5f, -0.5f, -0.5f)
                            close()
                            moveTo(10.0f, 20.5f)
                            curveToRelative(-0.28f, 0.0f, -0.5f, 0.22f, -0.5f, 0.5f)
                            reflectiveCurveToRelative(0.22f, 0.5f, 0.5f, 0.5f)
                            reflectiveCurveToRelative(0.5f, -0.22f, 0.5f, -0.5f)
                            reflectiveCurveToRelative(-0.22f, -0.5f, -0.5f, -0.5f)
                            close()
                            moveTo(10.0f, 3.5f)
                            curveToRelative(0.28f, 0.0f, 0.5f, -0.22f, 0.5f, -0.5f)
                            reflectiveCurveToRelative(-0.22f, -0.5f, -0.5f, -0.5f)
                            reflectiveCurveToRelative(-0.5f, 0.22f, -0.5f, 0.5f)
                            reflectiveCurveToRelative(0.22f, 0.5f, 0.5f, 0.5f)
                            close()
                            moveTo(10.0f, 7.0f)
                            curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                            reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                            reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                            reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                            close()
                            moveTo(10.0f, 12.5f)
                            curveToRelative(-0.83f, 0.0f, -1.5f, 0.67f, -1.5f, 1.5f)
                            reflectiveCurveToRelative(0.67f, 1.5f, 1.5f, 1.5f)
                            reflectiveCurveToRelative(1.5f, -0.67f, 1.5f, -1.5f)
                            reflectiveCurveToRelative(-0.67f, -1.5f, -1.5f, -1.5f)
                            close()
                            moveTo(18.0f, 13.0f)
                            curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                            reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                            reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                            reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                            close()
                            moveTo(18.0f, 17.0f)
                            curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                            reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                            reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                            reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                            close()
                            moveTo(18.0f, 9.0f)
                            curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                            reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                            reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                            reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                            close()
                            moveTo(18.0f, 5.0f)
                            curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                            reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                            reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                            reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                            close()
                            moveTo(21.0f, 13.5f)
                            curveToRelative(-0.28f, 0.0f, -0.5f, 0.22f, -0.5f, 0.5f)
                            reflectiveCurveToRelative(0.22f, 0.5f, 0.5f, 0.5f)
                            reflectiveCurveToRelative(0.5f, -0.22f, 0.5f, -0.5f)
                            reflectiveCurveToRelative(-0.22f, -0.5f, -0.5f, -0.5f)
                            close()
                            moveTo(14.0f, 17.0f)
                            curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                            reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                            reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                            reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                            close()
                            moveTo(14.0f, 20.5f)
                            curveToRelative(-0.28f, 0.0f, -0.5f, 0.22f, -0.5f, 0.5f)
                            reflectiveCurveToRelative(0.22f, 0.5f, 0.5f, 0.5f)
                            reflectiveCurveToRelative(0.5f, -0.22f, 0.5f, -0.5f)
                            reflectiveCurveToRelative(-0.22f, -0.5f, -0.5f, -0.5f)
                            close()
                            moveTo(10.0f, 8.5f)
                            curveToRelative(-0.83f, 0.0f, -1.5f, 0.67f, -1.5f, 1.5f)
                            reflectiveCurveToRelative(0.67f, 1.5f, 1.5f, 1.5f)
                            reflectiveCurveToRelative(1.5f, -0.67f, 1.5f, -1.5f)
                            reflectiveCurveToRelative(-0.67f, -1.5f, -1.5f, -1.5f)
                            close()
                            moveTo(10.0f, 17.0f)
                            curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                            reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                            reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                            reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                            close()
                            moveTo(14.0f, 12.5f)
                            curveToRelative(-0.83f, 0.0f, -1.5f, 0.67f, -1.5f, 1.5f)
                            reflectiveCurveToRelative(0.67f, 1.5f, 1.5f, 1.5f)
                            reflectiveCurveToRelative(1.5f, -0.67f, 1.5f, -1.5f)
                            reflectiveCurveToRelative(-0.67f, -1.5f, -1.5f, -1.5f)
                            close()
                            moveTo(14.0f, 8.5f)
                            curveToRelative(-0.83f, 0.0f, -1.5f, 0.67f, -1.5f, 1.5f)
                            reflectiveCurveToRelative(0.67f, 1.5f, 1.5f, 1.5f)
                            reflectiveCurveToRelative(1.5f, -0.67f, 1.5f, -1.5f)
                            reflectiveCurveToRelative(-0.67f, -1.5f, -1.5f, -1.5f)
                            close()   }
            }.build()
    }

    val Notifications: ImageVector by lazy {
            ImageVector.Builder(
                name = "AppIcons.Notifications",
                defaultWidth = 24.dp, defaultHeight = 24.dp,
                viewportWidth = 24f, viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                            moveTo(12.0f, 22.0f)
                            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                            horizontalLineToRelative(-4.0f)
                            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                            close()
                            moveTo(18.0f, 16.0f)
                            verticalLineToRelative(-5.0f)
                            curveToRelative(0.0f, -3.07f, -1.63f, -5.64f, -4.5f, -6.32f)
                            lineTo(13.5f, 4.0f)
                            curveToRelative(0.0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
                            reflectiveCurveToRelative(-1.5f, 0.67f, -1.5f, 1.5f)
                            verticalLineToRelative(0.68f)
                            curveTo(7.64f, 5.36f, 6.0f, 7.92f, 6.0f, 11.0f)
                            verticalLineToRelative(5.0f)
                            lineToRelative(-2.0f, 2.0f)
                            verticalLineToRelative(1.0f)
                            horizontalLineToRelative(16.0f)
                            verticalLineToRelative(-1.0f)
                            lineToRelative(-2.0f, -2.0f)
                            close()
                            moveTo(16.0f, 17.0f)
                            lineTo(8.0f, 17.0f)
                            verticalLineToRelative(-6.0f)
                            curveToRelative(0.0f, -2.48f, 1.51f, -4.5f, 4.0f, -4.5f)
                            reflectiveCurveToRelative(4.0f, 2.02f, 4.0f, 4.5f)
                            verticalLineToRelative(6.0f)
                            close()   }
            }.build()
    }

    val Palette: ImageVector by lazy {
            ImageVector.Builder(
                name = "AppIcons.Palette",
                defaultWidth = 24.dp, defaultHeight = 24.dp,
                viewportWidth = 24f, viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                            moveTo(12.0f, 22.0f)
                            curveTo(6.49f, 22.0f, 2.0f, 17.51f, 2.0f, 12.0f)
                            reflectiveCurveTo(6.49f, 2.0f, 12.0f, 2.0f)
                            reflectiveCurveToRelative(10.0f, 4.04f, 10.0f, 9.0f)
                            curveToRelative(0.0f, 3.31f, -2.69f, 6.0f, -6.0f, 6.0f)
                            horizontalLineToRelative(-1.77f)
                            curveToRelative(-0.28f, 0.0f, -0.5f, 0.22f, -0.5f, 0.5f)
                            curveToRelative(0.0f, 0.12f, 0.05f, 0.23f, 0.13f, 0.33f)
                            curveToRelative(0.41f, 0.47f, 0.64f, 1.06f, 0.64f, 1.67f)
                            curveTo(14.5f, 20.88f, 13.38f, 22.0f, 12.0f, 22.0f)
                            close()
                            moveTo(12.0f, 4.0f)
                            curveToRelative(-4.41f, 0.0f, -8.0f, 3.59f, -8.0f, 8.0f)
                            reflectiveCurveToRelative(3.59f, 8.0f, 8.0f, 8.0f)
                            curveToRelative(0.28f, 0.0f, 0.5f, -0.22f, 0.5f, -0.5f)
                            curveToRelative(0.0f, -0.16f, -0.08f, -0.28f, -0.14f, -0.35f)
                            curveToRelative(-0.41f, -0.46f, -0.63f, -1.05f, -0.63f, -1.65f)
                            curveToRelative(0.0f, -1.38f, 1.12f, -2.5f, 2.5f, -2.5f)
                            horizontalLineTo(16.0f)
                            curveToRelative(2.21f, 0.0f, 4.0f, -1.79f, 4.0f, -4.0f)
                            curveTo(20.0f, 7.14f, 16.41f, 4.0f, 12.0f, 4.0f)
                            close()   }
            }.build()
    }

    val RotateRight: ImageVector by lazy {
            ImageVector.Builder(
                name = "AppIcons.RotateRight",
                defaultWidth = 24.dp, defaultHeight = 24.dp,
                viewportWidth = 24f, viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                            moveTo(15.55f, 5.55f)
                            lineTo(11.0f, 1.0f)
                            verticalLineToRelative(3.07f)
                            curveTo(7.06f, 4.56f, 4.0f, 7.92f, 4.0f, 12.0f)
                            reflectiveCurveToRelative(3.05f, 7.44f, 7.0f, 7.93f)
                            verticalLineToRelative(-2.02f)
                            curveToRelative(-2.84f, -0.48f, -5.0f, -2.94f, -5.0f, -5.91f)
                            reflectiveCurveToRelative(2.16f, -5.43f, 5.0f, -5.91f)
                            lineTo(11.0f, 10.0f)
                            lineToRelative(4.55f, -4.45f)
                            close()
                            moveTo(19.93f, 11.0f)
                            curveToRelative(-0.17f, -1.39f, -0.72f, -2.73f, -1.62f, -3.89f)
                            lineToRelative(-1.42f, 1.42f)
                            curveToRelative(0.54f, 0.75f, 0.88f, 1.6f, 1.02f, 2.47f)
                            horizontalLineToRelative(2.02f)
                            close()
                            moveTo(13.0f, 17.9f)
                            verticalLineToRelative(2.02f)
                            curveToRelative(1.39f, -0.17f, 2.74f, -0.71f, 3.9f, -1.61f)
                            lineToRelative(-1.44f, -1.44f)
                            curveToRelative(-0.75f, 0.54f, -1.59f, 0.89f, -2.46f, 1.03f)
                            close()
                            moveTo(16.89f, 15.48f)
                            lineToRelative(1.42f, 1.41f)
                            curveToRelative(0.9f, -1.16f, 1.45f, -2.5f, 1.62f, -3.89f)
                            horizontalLineToRelative(-2.02f)
                            curveToRelative(-0.14f, 0.87f, -0.48f, 1.72f, -1.02f, 2.48f)
                            close()   }
            }.build()
    }

    val Settings: ImageVector by lazy {
            ImageVector.Builder(
                name = "AppIcons.Settings",
                defaultWidth = 24.dp, defaultHeight = 24.dp,
                viewportWidth = 24f, viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                            moveTo(19.43f, 12.98f)
                            curveToRelative(0.04f, -0.32f, 0.07f, -0.64f, 0.07f, -0.98f)
                            curveToRelative(0.0f, -0.34f, -0.03f, -0.66f, -0.07f, -0.98f)
                            lineToRelative(2.11f, -1.65f)
                            curveToRelative(0.19f, -0.15f, 0.24f, -0.42f, 0.12f, -0.64f)
                            lineToRelative(-2.0f, -3.46f)
                            curveToRelative(-0.09f, -0.16f, -0.26f, -0.25f, -0.44f, -0.25f)
                            curveToRelative(-0.06f, 0.0f, -0.12f, 0.01f, -0.17f, 0.03f)
                            lineToRelative(-2.49f, 1.0f)
                            curveToRelative(-0.52f, -0.4f, -1.08f, -0.73f, -1.69f, -0.98f)
                            lineToRelative(-0.38f, -2.65f)
                            curveTo(14.46f, 2.18f, 14.25f, 2.0f, 14.0f, 2.0f)
                            horizontalLineToRelative(-4.0f)
                            curveToRelative(-0.25f, 0.0f, -0.46f, 0.18f, -0.49f, 0.42f)
                            lineToRelative(-0.38f, 2.65f)
                            curveToRelative(-0.61f, 0.25f, -1.17f, 0.59f, -1.69f, 0.98f)
                            lineToRelative(-2.49f, -1.0f)
                            curveToRelative(-0.06f, -0.02f, -0.12f, -0.03f, -0.18f, -0.03f)
                            curveToRelative(-0.17f, 0.0f, -0.34f, 0.09f, -0.43f, 0.25f)
                            lineToRelative(-2.0f, 3.46f)
                            curveToRelative(-0.13f, 0.22f, -0.07f, 0.49f, 0.12f, 0.64f)
                            lineToRelative(2.11f, 1.65f)
                            curveToRelative(-0.04f, 0.32f, -0.07f, 0.65f, -0.07f, 0.98f)
                            curveToRelative(0.0f, 0.33f, 0.03f, 0.66f, 0.07f, 0.98f)
                            lineToRelative(-2.11f, 1.65f)
                            curveToRelative(-0.19f, 0.15f, -0.24f, 0.42f, -0.12f, 0.64f)
                            lineToRelative(2.0f, 3.46f)
                            curveToRelative(0.09f, 0.16f, 0.26f, 0.25f, 0.44f, 0.25f)
                            curveToRelative(0.06f, 0.0f, 0.12f, -0.01f, 0.17f, -0.03f)
                            lineToRelative(2.49f, -1.0f)
                            curveToRelative(0.52f, 0.4f, 1.08f, 0.73f, 1.69f, 0.98f)
                            lineToRelative(0.38f, 2.65f)
                            curveToRelative(0.03f, 0.24f, 0.24f, 0.42f, 0.49f, 0.42f)
                            horizontalLineToRelative(4.0f)
                            curveToRelative(0.25f, 0.0f, 0.46f, -0.18f, 0.49f, -0.42f)
                            lineToRelative(0.38f, -2.65f)
                            curveToRelative(0.61f, -0.25f, 1.17f, -0.59f, 1.69f, -0.98f)
                            lineToRelative(2.49f, 1.0f)
                            curveToRelative(0.06f, 0.02f, 0.12f, 0.03f, 0.18f, 0.03f)
                            curveToRelative(0.17f, 0.0f, 0.34f, -0.09f, 0.43f, -0.25f)
                            lineToRelative(2.0f, -3.46f)
                            curveToRelative(0.12f, -0.22f, 0.07f, -0.49f, -0.12f, -0.64f)
                            lineToRelative(-2.11f, -1.65f)
                            close()
                            moveTo(17.45f, 11.27f)
                            curveToRelative(0.04f, 0.31f, 0.05f, 0.52f, 0.05f, 0.73f)
                            curveToRelative(0.0f, 0.21f, -0.02f, 0.43f, -0.05f, 0.73f)
                            lineToRelative(-0.14f, 1.13f)
                            lineToRelative(0.89f, 0.7f)
                            lineToRelative(1.08f, 0.84f)
                            lineToRelative(-0.7f, 1.21f)
                            lineToRelative(-1.27f, -0.51f)
                            lineToRelative(-1.04f, -0.42f)
                            lineToRelative(-0.9f, 0.68f)
                            curveToRelative(-0.43f, 0.32f, -0.84f, 0.56f, -1.25f, 0.73f)
                            lineToRelative(-1.06f, 0.43f)
                            lineToRelative(-0.16f, 1.13f)
                            lineToRelative(-0.2f, 1.35f)
                            horizontalLineToRelative(-1.4f)
                            lineToRelative(-0.19f, -1.35f)
                            lineToRelative(-0.16f, -1.13f)
                            lineToRelative(-1.06f, -0.43f)
                            curveToRelative(-0.43f, -0.18f, -0.83f, -0.41f, -1.23f, -0.71f)
                            lineToRelative(-0.91f, -0.7f)
                            lineToRelative(-1.06f, 0.43f)
                            lineToRelative(-1.27f, 0.51f)
                            lineToRelative(-0.7f, -1.21f)
                            lineToRelative(1.08f, -0.84f)
                            lineToRelative(0.89f, -0.7f)
                            lineToRelative(-0.14f, -1.13f)
                            curveToRelative(-0.03f, -0.31f, -0.05f, -0.54f, -0.05f, -0.74f)
                            reflectiveCurveToRelative(0.02f, -0.43f, 0.05f, -0.73f)
                            lineToRelative(0.14f, -1.13f)
                            lineToRelative(-0.89f, -0.7f)
                            lineToRelative(-1.08f, -0.84f)
                            lineToRelative(0.7f, -1.21f)
                            lineToRelative(1.27f, 0.51f)
                            lineToRelative(1.04f, 0.42f)
                            lineToRelative(0.9f, -0.68f)
                            curveToRelative(0.43f, -0.32f, 0.84f, -0.56f, 1.25f, -0.73f)
                            lineToRelative(1.06f, -0.43f)
                            lineToRelative(0.16f, -1.13f)
                            lineToRelative(0.2f, -1.35f)
                            horizontalLineToRelative(1.39f)
                            lineToRelative(0.19f, 1.35f)
                            lineToRelative(0.16f, 1.13f)
                            lineToRelative(1.06f, 0.43f)
                            curveToRelative(0.43f, 0.18f, 0.83f, 0.41f, 1.23f, 0.71f)
                            lineToRelative(0.91f, 0.7f)
                            lineToRelative(1.06f, -0.43f)
                            lineToRelative(1.27f, -0.51f)
                            lineToRelative(0.7f, 1.21f)
                            lineToRelative(-1.07f, 0.85f)
                            lineToRelative(-0.89f, 0.7f)
                            lineToRelative(0.14f, 1.13f)
                            close()
                            moveTo(12.0f, 8.0f)
                            curveToRelative(-2.21f, 0.0f, -4.0f, 1.79f, -4.0f, 4.0f)
                            reflectiveCurveToRelative(1.79f, 4.0f, 4.0f, 4.0f)
                            reflectiveCurveToRelative(4.0f, -1.79f, 4.0f, -4.0f)
                            reflectiveCurveToRelative(-1.79f, -4.0f, -4.0f, -4.0f)
                            close()
                            moveTo(12.0f, 14.0f)
                            curveToRelative(-1.1f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
                            reflectiveCurveToRelative(0.9f, -2.0f, 2.0f, -2.0f)
                            reflectiveCurveToRelative(2.0f, 0.9f, 2.0f, 2.0f)
                            reflectiveCurveToRelative(-0.9f, 2.0f, -2.0f, 2.0f)
                            close()   }
            }.build()
    }

    val Shield: ImageVector by lazy {
            ImageVector.Builder(
                name = "AppIcons.Shield",
                defaultWidth = 24.dp, defaultHeight = 24.dp,
                viewportWidth = 24f, viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                            moveTo(12.0f, 2.0f)
                            lineTo(4.0f, 5.0f)
                            verticalLineToRelative(6.09f)
                            curveToRelative(0.0f, 5.05f, 3.41f, 9.76f, 8.0f, 10.91f)
                            curveToRelative(4.59f, -1.15f, 8.0f, -5.86f, 8.0f, -10.91f)
                            verticalLineTo(5.0f)
                            lineTo(12.0f, 2.0f)
                            close()
                            moveTo(18.0f, 11.09f)
                            curveToRelative(0.0f, 4.0f, -2.55f, 7.7f, -6.0f, 8.83f)
                            curveToRelative(-3.45f, -1.13f, -6.0f, -4.82f, -6.0f, -8.83f)
                            verticalLineToRelative(-4.7f)
                            lineToRelative(6.0f, -2.25f)
                            lineToRelative(6.0f, 2.25f)
                            verticalLineTo(11.09f)
                            close()   }
            }.build()
    }

    val Speed: ImageVector by lazy {
            ImageVector.Builder(
                name = "AppIcons.Speed",
                defaultWidth = 24.dp, defaultHeight = 24.dp,
                viewportWidth = 24f, viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                            moveTo(20.38f, 8.57f)
                            lineToRelative(-1.23f, 1.85f)
                            arcToRelative(8.0f, 8.0f, 0.0f, false, true, -0.22f, 7.58f)
                            horizontalLineTo(5.07f)
                            arcTo(8.0f, 8.0f, 0.0f, false, true, 15.58f, 6.85f)
                            lineToRelative(1.85f, -1.23f)
                            arcTo(10.0f, 10.0f, 0.0f, false, false, 3.35f, 19.0f)
                            arcToRelative(2.0f, 2.0f, 0.0f, false, false, 1.72f, 1.0f)
                            horizontalLineToRelative(13.85f)
                            arcToRelative(2.0f, 2.0f, 0.0f, false, false, 1.74f, -1.0f)
                            arcToRelative(10.0f, 10.0f, 0.0f, false, false, -0.27f, -10.44f)
                            close()   }
            }.build()
    }

    val WaterDrop: ImageVector by lazy {
            ImageVector.Builder(
                name = "AppIcons.WaterDrop",
                defaultWidth = 24.dp, defaultHeight = 24.dp,
                viewportWidth = 24f, viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                            moveTo(12.0f, 2.0f)
                            curveTo(6.67f, 6.55f, 4.0f, 10.47f, 4.0f, 13.8f)
                            curveTo(4.0f, 18.78f, 7.8f, 22.0f, 12.0f, 22.0f)
                            reflectiveCurveToRelative(8.0f, -3.22f, 8.0f, -8.2f)
                            curveTo(20.0f, 10.47f, 17.33f, 6.55f, 12.0f, 2.0f)
                            close()   }
            }.build()
    }

    val Image: ImageVector by lazy {
            ImageVector.Builder(
                name = "AppIcons.Image",
                defaultWidth = 24.dp, defaultHeight = 24.dp,
                viewportWidth = 24f, viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                            moveTo(21.0f, 19.0f)
                            verticalLineTo(5.0f)
                            curveTo(21.0f, 3.9f, 20.1f, 3.0f, 19.0f, 3.0f)
                            horizontalLineTo(5.0f)
                            curveTo(3.9f, 3.0f, 3.0f, 3.9f, 3.0f, 5.0f)
                            verticalLineToRelative(14.0f)
                            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                            horizontalLineToRelative(14.0f)
                            curveTo(20.1f, 21.0f, 21.0f, 20.1f, 21.0f, 19.0f)
                            close()
                            moveTo(8.5f, 13.5f)
                            lineToRelative(2.5f, 3.01f)
                            lineTo(14.5f, 12.0f)
                            lineToRelative(4.5f, 6.0f)
                            horizontalLineTo(5.0f)
                            lineToRelative(3.5f, -4.5f)
                            close()   }
            }.build()
    }

    val Widgets: ImageVector by lazy {
            ImageVector.Builder(
                name = "AppIcons.Widgets",
                defaultWidth = 24.dp, defaultHeight = 24.dp,
                viewportWidth = 24f, viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                            moveTo(16.66f, 4.52f)
                            lineToRelative(2.83f, 2.83f)
                            lineToRelative(-2.83f, 2.83f)
                            lineToRelative(-2.83f, -2.83f)
                            lineToRelative(2.83f, -2.83f)
                            moveTo(9.0f, 5.0f)
                            verticalLineToRelative(4.0f)
                            lineTo(5.0f, 9.0f)
                            lineTo(5.0f, 5.0f)
                            horizontalLineToRelative(4.0f)
                            moveToRelative(10.0f, 10.0f)
                            verticalLineToRelative(4.0f)
                            horizontalLineToRelative(-4.0f)
                            verticalLineToRelative(-4.0f)
                            horizontalLineToRelative(4.0f)
                            moveTo(9.0f, 15.0f)
                            verticalLineToRelative(4.0f)
                            lineTo(5.0f, 19.0f)
                            verticalLineToRelative(-4.0f)
                            horizontalLineToRelative(4.0f)
                            moveToRelative(7.66f, -13.31f)
                            lineTo(11.0f, 7.34f)
                            lineTo(16.66f, 13.0f)
                            lineToRelative(5.66f, -5.66f)
                            lineToRelative(-5.66f, -5.65f)
                            close()
                            moveTo(11.0f, 3.0f)
                            lineTo(3.0f, 3.0f)
                            verticalLineToRelative(8.0f)
                            horizontalLineToRelative(8.0f)
                            lineTo(11.0f, 3.0f)
                            close()
                            moveTo(21.0f, 13.0f)
                            horizontalLineToRelative(-8.0f)
                            verticalLineToRelative(8.0f)
                            horizontalLineToRelative(8.0f)
                            verticalLineToRelative(-8.0f)
                            close()
                            moveTo(11.0f, 13.0f)
                            lineTo(3.0f, 13.0f)
                            verticalLineToRelative(8.0f)
                            horizontalLineToRelative(8.0f)
                            verticalLineToRelative(-8.0f)
                            close()   }
            }.build()
    }
}
