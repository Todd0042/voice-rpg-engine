package androidx.compose.ui.platform

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

val LocalContext = staticCompositionLocalOf { Context() }
val LocalConfiguration = compositionLocalOf { Configuration() }
