package com.expiryguard.app.ui.liquid.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp

/**
 * Motion tokens.
 *
 * Duration / easing / spring values are derived from the `motion-web` reference
 * motion system and translated from its Framer-Motion `(stiffness, damping)`
 * notation into Compose's `(dampingRatio, stiffness)` notation using
 *
 *     dampingRatio = damping / (2 * sqrt(stiffness * mass))
 *
 * The derived ratios are then rounded to stable, near-critical values so the
 * same token feels identical across the whole app. Keep every animation in the
 * template reading from this file — no ad-hoc `spring(...)` at call sites.
 */
object Motion {

    /**
     * Global spring damping, 0f..1f (0.5 = design default).
     *
     * The settings slider writes this; every spring token below reads it
     * through [DampedRatio], so one knob retunes the whole app's feel. Lower
     * values land bouncier, higher values settle more critically.
     */
    val motionDamping = mutableFloatStateOf(0.5f)

    /**
     * Scales a spring's damping ratio by the current global damping knob.
     * Read during composition so a setting change recomposes the spec users.
     */
    fun DampedRatio(base: Float): Float {
        val d = motionDamping.floatValue.coerceIn(0f, 1f)
        return (base * lerp(0.72f, 1.28f, d)).coerceIn(0.25f, 1.25f)
    }

    // -----------------------------------------------------------------------
    // Duration scale (ms) — motion-web `motion-tokens.md`
    // -----------------------------------------------------------------------
    object Duration {
        /** Cursor / tooltip feedback. */
        const val Instant = 90
        /** Hover, button active, toggle. */
        const val Fast = 180
        /** Card expand, panel slide, drawer. */
        const val Standard = 320
        /** Section reveal, image open. */
        const val Medium = 460
        /** Page enter, hero reveal, route transition. */
        const val Slow = 700
        /** Cold open, curtain wipe. */
        const val Cinematic = 1200
    }

    // -----------------------------------------------------------------------
    // Easing dictionary — exact cubic-bezier values
    // -----------------------------------------------------------------------
    /** Fast in, soft landing. The default for reveals. */
    val EaseOutExpo: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    /** Slow start, hard exit. Exits only. */
    val EaseInExpo: Easing = CubicBezierEasing(0.7f, 0f, 0.84f, 0f)

    /** Symmetrical smooth. Auto-play loops. */
    val EaseInOutQuart: Easing = CubicBezierEasing(0.76f, 0f, 0.24f, 1f)

    /** Overshoot landing. Playful hovers. */
    val EaseOutBack: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

    /** Crisp mechanical decel. */
    val EaseOutCirc: Easing = CubicBezierEasing(0f, 0.55f, 0.45f, 1f)

    /** Hard brand slam. */
    val EaseSharp: Easing = CubicBezierEasing(0.65f, 0f, 0.45f, 1f)

    /** Overshoots both ends. Use rarely. */
    val EaseExpressive: Easing = CubicBezierEasing(0.68f, -0.6f, 0.32f, 1.6f)

    // -----------------------------------------------------------------------
    // Spring set — derived from motion-web spring presets
    // -----------------------------------------------------------------------
    /** Soft, slow, no bounce. Ambient reveals. (~gentle) */
    fun <T> gentle(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        spring(DampedRatio(0.90f), 240f, visibilityThreshold)

    /** Natural feel. Card hovers, drawer slides. (~default) */
    fun <T> standard(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        spring(DampedRatio(0.82f), 520f, visibilityThreshold)

    /** Premium fast snap. Button feedback, tabs. (~snappy) */
    fun <T> snappy(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        spring(DampedRatio(0.76f), 900f, visibilityThreshold)

    /** Playful overshoot. Game-like icons, confetti. (~bouncy) */
    fun <T> bouncy(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        spring(DampedRatio(0.46f), 420f, visibilityThreshold)

    /** Weighted, sluggish. Large hero panels. (~heavy) */
    fun <T> heavy(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        spring(DampedRatio(1.0f), 180f, visibilityThreshold)

    /** Near-instant. Mouse-tracking cursors, drag handles. (~stiff) */
    fun <T> stiff(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        spring(DampedRatio(0.90f), 2200f, visibilityThreshold)

    // -----------------------------------------------------------------------
    // Concrete specs used by components
    // -----------------------------------------------------------------------
    val Press: FiniteAnimationSpec<Float> = snappy(0.001f)
    val TabSlide: FiniteAnimationSpec<Float> = snappy(0.001f)
    val PanelExpand: FiniteAnimationSpec<Float> = standard(0.01f)
    val HeroReveal: FiniteAnimationSpec<Float> = gentle(0.01f)
    val OffsetSnap: FiniteAnimationSpec<IntOffset> = snappy(IntOffset(1, 1))

    // -----------------------------------------------------------------------
    // Stagger — delay per item, in ms
    // -----------------------------------------------------------------------
    object Stagger {
        /** Dense data grids. */
        const val TightGrid = 40
        /** Card lists, feature grids, nav links. */
        const val StandardList = 70
        /** Text line reveals, article sections. */
        const val EditorialLine = 90
        /** Award-site heroes, broadside reveals. */
        const val DramaticCascade = 150
    }

    /**
     * Frame-rate independent exponential follow, the Compose equivalent of
     * motion-web §7.1. `tightness` is in 1/s — NOT a per-frame fraction.
     *
     * Use this (with [standard]/[gentle]) for anything that must arrive and
     * *stop*: camera-like scroll, a rail under the active tab, a panel levelling.
     * Use [bouncy]/[snappy] for things that should land with weight.
     */
    object Follow {
        const val CursorTightness = 18f
        const val CameraTightness = 4.5f
        const val RailTightness = 12f
        const val LevelTightness = 2.2f
    }

    /** Critical damping ratio, `c = 2*sqrt(k)`. Zero overshoot. */
    const val CriticalDamping = Spring.DampingRatioNoBouncy
}
