package dev.xuanran.xposedscaffold.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import dev.xuanran.xposedscaffold.ui.screens.FeaturesScreen
import dev.xuanran.xposedscaffold.ui.screens.HomeScreen

@Composable
fun ModuleApp() {
    var page by remember { mutableIntStateOf(0) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 10.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                NavigationBar(containerColor = androidx.compose.ui.graphics.Color.Transparent) {
                    NavigationBarItem(
                        selected = page == 0,
                        onClick = { page = 0 },
                        icon = { Icon(Icons.Outlined.Home, "主页") },
                        label = { Text("主页", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                    NavigationBarItem(
                        selected = page == 1,
                        onClick = { page = 1 },
                        icon = { Icon(Icons.Outlined.Tune, "功能") },
                        label = { Text("功能", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        when (page) {
            0 -> HomeScreen(Modifier.padding(padding))
            else -> FeaturesScreen(Modifier.padding(padding))
        }
    }
}
