/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.utils

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import androidx.compose.ui.graphics.Shape
import com.convx.music.constants.AppLanguageKey
import com.convx.music.constants.SYSTEM_DEFAULT
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
}

@Suppress("DEPRECATION")
fun setAppLocale(context: Context, locale: Locale) {
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}

/**
 * The app's effective locale: the user's explicit choice from Settings, or
 * Simplified Chinese (zh-CN) when unset / set to "follow system".
 *
 * Convx is forced to default to Simplified Chinese so the UI no longer
 * follows the system language for fresh installs.
 */
fun Context.effectiveAppLocale(): Locale {
    val stored = dataStore[AppLanguageKey]
    return stored
        ?.takeUnless { it == SYSTEM_DEFAULT }
        ?.let { Locale.forLanguageTag(it) }
        ?: Locale.forLanguageTag("zh-CN")
}

/**
 * Returns a context whose configuration is pinned to [effectiveAppLocale].
 * Call from [android.app.Activity.attachBaseContext] so every Activity (and
 * therefore every Compose screen) renders in Simplified Chinese by default,
 * regardless of the system language. Works on all API levels, including
 * Android 13+ where per-app locales are otherwise system-managed.
 */
fun Context.applyForcedLocale(): Context {
    val config = Configuration(resources.configuration)
    config.setLocale(effectiveAppLocale())
    return createConfigurationContext(config)
}

// A radius only ever yields three distinct shapes (single / first / last), but
// this is called from inside ~29 lazy-list item lambdas, so it used to allocate
// a fresh AbsoluteSmoothCornerShape per item per composition. The identity churn
// also missed Compose's outline cache — which keys on the shape instance — so
// every row rebuilt its corner Path on every draw. Cache per radius instead.
private val listItemShapes = ConcurrentHashMap<Dp, Array<Shape>>()

fun listItemShape(index: Int, count: Int, radius: Dp = 24.dp): Shape {
    val shapes = listItemShapes.getOrPut(radius) {
        val smoothness = 60
        arrayOf(
            AbsoluteSmoothCornerShape(
                cornerRadiusTL = radius, smoothnessAsPercentTL = smoothness,
                cornerRadiusTR = radius, smoothnessAsPercentTR = smoothness,
                cornerRadiusBL = radius, smoothnessAsPercentBL = smoothness,
                cornerRadiusBR = radius, smoothnessAsPercentBR = smoothness
            ),
            AbsoluteSmoothCornerShape(
                cornerRadiusTL = radius, smoothnessAsPercentTL = smoothness,
                cornerRadiusTR = radius, smoothnessAsPercentTR = smoothness,
                cornerRadiusBL = 0.dp, smoothnessAsPercentBL = 0,
                cornerRadiusBR = 0.dp, smoothnessAsPercentBR = 0
            ),
            AbsoluteSmoothCornerShape(
                cornerRadiusTL = 0.dp, smoothnessAsPercentTL = 0,
                cornerRadiusTR = 0.dp, smoothnessAsPercentTR = 0,
                cornerRadiusBL = radius, smoothnessAsPercentBL = smoothness,
                cornerRadiusBR = radius, smoothnessAsPercentBR = smoothness
            ),
        )
    }
    return when {
        count == 1 -> shapes[0]
        index == 0 -> shapes[1]
        index == count - 1 -> shapes[2]
        else -> RectangleShape
    }
}
