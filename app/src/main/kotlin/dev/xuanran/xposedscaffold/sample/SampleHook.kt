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
    override val options = listOf(
        BooleanOption("include_class_name", "Include class name", default = true),
        StringOption("log_tag", "Log tag", "Used for Logcat output.", "XposedScaffold"),
    )

    override fun install(context: HookContext) {
        val method = Activity::class.java.getDeclaredMethod("onResume")
        context.hook(method) {
            after {
                Log.d("XposedScaffold", "Resumed: ${thisObject?.javaClass?.name}")
            }
        }
    }
}
