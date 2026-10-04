package com.rustanovych.kidsoutfit.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Forces dark ([darkIcons] = true) or light status-bar icons while this is in the composition, and
 * puts the previous appearance back when it leaves.
 *
 * The edge-to-edge default follows the system theme, but a screen that draws a picture behind the
 * status bar needs icons that suit the picture, not the theme.
 */
@Composable
fun StatusBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    val activity = LocalContext.current.findActivity()
    if (view.isInEditMode || activity == null) return
    DisposableEffect(darkIcons) {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = darkIcons
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}
