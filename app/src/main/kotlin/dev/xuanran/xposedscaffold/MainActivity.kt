package dev.xuanran.xposedscaffold

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.xuanran.xposed.generated.createHooks
import dev.xuanran.xposed.runtime.HookRegistry
import dev.xuanran.xposedscaffold.ui.ModuleApp
import dev.xuanran.xposedscaffold.ui.theme.ModuleTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<ModuleViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (HookRegistry.all().isEmpty()) HookRegistry.register(createHooks())
        setContent { ModuleTheme { ModuleApp(viewModel) } }
    }
}
