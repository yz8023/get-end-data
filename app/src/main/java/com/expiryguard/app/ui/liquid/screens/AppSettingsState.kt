package com.expiryguard.app.ui.liquid.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.expiryguard.app.ui.liquid.theme.PaletteId
import com.expiryguard.app.ui.liquid.theme.ThemeMode
import com.expiryguard.app.ui.liquid.theme.VisualMode

/**
 * Every user-facing knob the template exposes, persisted so a fresh launch
 * restores the exact look the user configured. This is the only place the app
 * writes to disk — business features added on top should follow the same pattern
 * (one state holder, one preferences file).
 */
@Stable
class AppSettingsState(private val prefs: Context) {

    private val store = prefs.getSharedPreferences("liquidui.settings", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(
        runCatching { ThemeMode.valueOf(store.getString(KEY_THEME, ThemeMode.System.name)!!) }
            .getOrDefault(ThemeMode.System)
    )
        private set

    var palette by mutableStateOf(
        runCatching { PaletteId.valueOf(store.getString(KEY_PALETTE, PaletteId.Aurora.name)!!) }
            .getOrDefault(PaletteId.Aurora)
    )
        private set

    var visualMode by mutableStateOf(
        runCatching { VisualMode.valueOf(store.getString(KEY_VISUAL_MODE, legacyVisualMode().name)!!) }
            .getOrDefault(legacyVisualMode())
    )
        private set

    /** True when the old independent knob would have shown the glass look. */
    private fun legacyVisualMode(): VisualMode {
        val glass = store.getBoolean(KEY_GLASS_ENABLED, true)
        val dyn = store.getBoolean(KEY_DYNAMIC, false)
        return when {
            dyn -> VisualMode.Md3
            glass -> VisualMode.Glass
            else -> VisualMode.Normal
        }
    }

    var reducedMotion by mutableStateOf(store.getBoolean(KEY_REDUCED, false))
        private set

    var motionDamping by mutableFloatStateOf(store.getFloat(KEY_DAMPING, 0.5f))
        private set

    var wallpaperBackground by mutableStateOf(store.getBoolean(KEY_WALLPAPER, false))
        private set

    var glassOpacity by mutableFloatStateOf(
        if (store.contains(KEY_OPACITY)) store.getFloat(KEY_OPACITY, 0.55f)
        else store.getFloat(KEY_FROST, 0.55f)
    )
        private set

    var glassRefraction by mutableFloatStateOf(store.getFloat(KEY_REFRACTION, 1f))
        private set

    var cornerScale by mutableFloatStateOf(store.getFloat(KEY_CORNER, 1f))
        private set

    fun updateThemeMode(value: ThemeMode) {
        themeMode = value
        store.edit().putString(KEY_THEME, value.name).apply()
    }

    fun updatePalette(value: PaletteId) {
        palette = value
        store.edit().putString(KEY_PALETTE, value.name).apply()
    }

    fun updateVisualMode(value: VisualMode) {
        visualMode = value
        store.edit().putString(KEY_VISUAL_MODE, value.name).apply()
    }

    fun updateReducedMotion(value: Boolean) {
        reducedMotion = value
        store.edit().putBoolean(KEY_REDUCED, value).apply()
    }

    fun updateMotionDamping(value: Float) {
        motionDamping = value
        store.edit().putFloat(KEY_DAMPING, value).apply()
    }

    fun updateWallpaperBackground(value: Boolean) {
        wallpaperBackground = value
        store.edit().putBoolean(KEY_WALLPAPER, value).apply()
    }

    fun updateGlassOpacity(value: Float) {
        glassOpacity = value
        store.edit().putFloat(KEY_OPACITY, value).apply()
    }

    fun updateGlassRefraction(value: Float) {
        glassRefraction = value
        store.edit().putFloat(KEY_REFRACTION, value).apply()
    }

    fun updateCornerScale(value: Float) {
        cornerScale = value
        store.edit().putFloat(KEY_CORNER, value).apply()
    }

    fun reset() {
        updateThemeMode(ThemeMode.System)
        updatePalette(PaletteId.Aurora)
        updateVisualMode(VisualMode.Glass)
        updateReducedMotion(false)
        updateMotionDamping(0.5f)
        updateWallpaperBackground(false)
        updateGlassOpacity(0.55f)
        updateGlassRefraction(1f)
        updateCornerScale(1f)
    }

    private companion object {
        const val KEY_THEME = "themeMode"
        const val KEY_PALETTE = "palette"
        const val KEY_VISUAL_MODE = "visualMode"
        const val KEY_GLASS_ENABLED = "glassEnabled"
        const val KEY_DYNAMIC = "dynamicColor"
        const val KEY_REDUCED = "reducedMotion"
        const val KEY_DAMPING = "motionDamping"
        const val KEY_WALLPAPER = "wallpaperBackground"
        const val KEY_FROST = "glassFrost"
        const val KEY_OPACITY = "glassOpacity"
        const val KEY_REFRACTION = "glassRefraction"
        const val KEY_CORNER = "cornerScale"
    }
}

@Composable
fun rememberAppSettingsState(): AppSettingsState {
    val context = LocalContext.current.applicationContext
    return remember(context) { AppSettingsState(context) }
}
