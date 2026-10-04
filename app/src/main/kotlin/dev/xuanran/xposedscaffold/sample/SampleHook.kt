package dev.xuanran.xposedscaffold.sample

import android.app.Application
import android.widget.Toast
import dev.xuanran.xposed.api.HookContext
import dev.xuanran.xposed.api.HookItem
import dev.xuanran.xposed.api.RestartPolicy
import dev.xuanran.xposed.api.StringOption
import dev.xuanran.xposed.api.hook
import dev.xuanran.xposed.runtime.SwitchHook
import dev.xuanran.xposed.runtime.optionKey

@HookItem(
    id = "sample.application_on_create_toast",
    path = "Samples/Lifecycle",
    title = "Application onCreate Toast",
    description = "Shows a Toast after the target application's Application.onCreate().",
    keywords = ["sample", "application", "onCreate", "toast"],
    targetPackages = ["com.example.target"],
    targetProcesses = ["main"],
    restartPolicy = RestartPolicy.HOST,
)
object SampleHook : SwitchHook() {
    // 标准配置项会由设置页自动渲染；简单功能无需编写任何 Compose 页面。
    override val options = listOf(
        StringOption(
            key = "toast_text",
            title = "Toast text",
            description = "Text shown when the target application's main process starts.",
            default = "Hello from Xposed Scaffold",
        ),
    )

    override fun install(context: HookContext) {
        // Hook Android's stable Application lifecycle method, so this demo does not depend
        // on any target-private or obfuscated class. The main-process filter above means the
        // callback normally runs once per cold start rather than once for every Activity.
        val onCreate = Application::class.java.getDeclaredMethod("onCreate")
        val toastText = context.config.getString(
            optionKey(metadata.id, "toast_text"),
            "Hello from Xposed Scaffold",
        )
        context.hook(onCreate) {
            after {
                val application = thisObject as Application
                Toast.makeText(application, toastText, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
