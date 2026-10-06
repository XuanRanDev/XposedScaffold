package dev.xuanran.xposedscaffold.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import dev.xuanran.xposedscaffold.R
import dev.xuanran.xposedscaffold.ModuleViewModel
import dev.xuanran.xposedscaffold.ui.screens.FeaturesScreen
import dev.xuanran.xposedscaffold.ui.screens.HomeScreen

@Composable
fun ModuleApp(viewModel: ModuleViewModel) {
    // 页面数量很少时不引入 Navigation Compose，可减少模板依赖和路由样板代码。
    // 后续增加日志/设置等深层页面时，再把这个局部状态替换成 NavHost 即可。
    var page by remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionFeedback by viewModel.actionFeedback.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val successMessage = stringResource(R.string.action_completed)
    val failureMessage = stringResource(R.string.action_failed)
    LaunchedEffect(actionFeedback) {
        actionFeedback?.let {
            snackbarHostState.showSnackbar(
                if (it.error == null) "$successMessage: ${it.title}" else "$failureMessage: ${it.title}",
            )
            viewModel.consumeActionFeedback()
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                // navigationBarsPadding 防止胶囊导航被手势条遮挡；外层留白形成悬浮视觉。
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
                        icon = { Icon(painterResource(R.drawable.ic_home), stringResource(R.string.nav_home)) },
                        label = { Text(stringResource(R.string.nav_home), fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                    NavigationBarItem(
                        selected = page == 1,
                        onClick = { page = 1 },
                        icon = { Icon(painterResource(R.drawable.ic_tune), stringResource(R.string.nav_features)) },
                        label = { Text(stringResource(R.string.nav_features), fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        // Scaffold 已计算状态栏和底部导航占用空间，页面只消费 padding，避免各页面重复处理。
        when (page) {
            0 -> HomeScreen(uiState, Modifier.padding(padding))
            else -> FeaturesScreen(uiState, viewModel, Modifier.padding(padding))
        }
    }
}
