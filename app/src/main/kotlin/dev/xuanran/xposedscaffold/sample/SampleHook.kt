package dev.xuanran.xposedscaffold.sample

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import dev.xuanran.xposedscaffold.BuildConfig
import dev.xuanran.xposed.api.HookContext
import dev.xuanran.xposed.api.HookItem
import dev.xuanran.xposed.api.RestartPolicy
import dev.xuanran.xposed.api.StringOption
import dev.xuanran.xposed.api.hook
import dev.xuanran.xposed.runtime.ApiHook
import dev.xuanran.xposed.runtime.optionKey

@HookItem(
    id = "sample.activity_on_create_toast",
    path = "Samples/Lifecycle",
    title = "Activity onCreate Toast",
    description = "Shows a Toast after an Activity is created in the target application.",
    keywords = ["sample", "activity", "onCreate", "toast"],
    restartPolicy = RestartPolicy.HOST,
)
object SampleHook : ApiHook() {
    // 标准配置项会由设置页自动渲染；简单功能无需编写任何 Compose 页面。
    override val options = listOf(
        StringOption(
            key = "toast_text",
            title = "Toast text",
            description = "Text shown whenever an Activity is created.",
            default = "Hello from Xposed Scaffold",
        ),
    )

    override fun install(context: HookContext) {
        val onCreate = Activity::class.java.getDeclaredMethod("onCreate", Bundle::class.java)
        val toastText = context.config.getString(
            optionKey(metadata.id, "toast_text"),
            "Hello from Xposed Scaffold",
        )
        context.hook(onCreate) {
            after {
                val activity = thisObject as Activity
                context.bridge.log(
                    android.util.Log.INFO,
                    "XposedScaffold",
                    "Activity.onCreate callback reached: ${activity.javaClass.name}",
                )
                Toast.makeText(activity, toastText, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
