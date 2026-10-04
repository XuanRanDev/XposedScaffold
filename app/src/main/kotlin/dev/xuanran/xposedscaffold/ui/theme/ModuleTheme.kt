package dev.xuanran.xposedscaffold.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun ModuleTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme(
            primary = Color(0xFF9ED5A5),
            primaryContainer = Color(0xFF23502C),
            surfaceContainer = Color(0xFF1B211B),
        )
        else -> lightColorScheme(
            primary = Color(0xFF356A3F),
            primaryContainer = Color(0xFFB8F0BD),
            surfaceContainer = Color(0xFFEDF3E9),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
