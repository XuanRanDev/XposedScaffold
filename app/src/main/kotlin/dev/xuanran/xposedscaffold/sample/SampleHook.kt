package dev.xuanran.xposedscaffold.sample

import android.app.Activity
import android.util.Log
import dev.xuanran.xposed.api.HookContext
import dev.xuanran.xposed.api.HookItem
import dev.xuanran.xposed.api.RestartPolicy
import dev.xuanran.xposed.api.BooleanOption
import dev.xuanran.xposed.api.StringOption
import dev.xuanran.xposed.api.hook
import dev.xuanran.xposed.runtime.SwitchHook

@HookItem(
    id = "sample.activity_logger",
    path = "Samples/Lifecycle",
    title = "Activity lifecycle logger",
    description = "Logs every resumed Activity. Replace this sample with your first feature.",
    keywords = ["sample", "activity", "lifecycle"],
    targetPackages = ["com.example.target"],
    targetProcesses = ["main"],
    restartPolicy = RestartPolicy.HOST,
)
object SampleHook : SwitchHook() {
    // 标准配置项会由设置页自动渲染；简单功能无需编写任何 Compose 页面。
    override val options = listOf(
        BooleanOption("include_class_name", "Include class name", default = true),
        StringOption("log_tag", "Log tag", "Used for Logcat output.", "XposedScaffold"),
    )

    override fun install(context: HookContext) {
        // 这是不会依赖目标私有类的安全示例。真实功能通常通过 DexKit 取得混淆方法。
        val method = Activity::class.java.getDeclaredMethod("onResume")
        context.hook(method) {
            after {
                Log.d("XposedScaffold", "Resumed: ${thisObject?.javaClass?.name}")
            }
        }
    }
}
