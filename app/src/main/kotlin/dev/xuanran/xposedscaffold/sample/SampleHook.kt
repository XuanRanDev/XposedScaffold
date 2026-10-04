package dev.xuanran.xposedscaffold.sample

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import dev.xuanran.xposed.api.HookContext
import dev.xuanran.xposed.api.HookItem
import dev.xuanran.xposed.api.ModuleConfig
import dev.xuanran.xposed.api.RestartPolicy
import dev.xuanran.xposed.api.StringOption
import dev.xuanran.xposed.api.hook
import dev.xuanran.xposed.runtime.SwitchHook
import dev.xuanran.xposed.runtime.optionKey

@HookItem(
    id = "sample.activity_on_create_toast",
    path = "Samples/Lifecycle",
    title = "Activity onCreate Toast",
    description = "Shows a Toast after an Activity is created in the target application.",
    keywords = ["sample", "activity", "onCreate", "toast"],
    restartPolicy = RestartPolicy.HOST,
)
object SampleHook : SwitchHook() {
    // 标准配置项会由设置页自动渲染；简单功能无需编写任何 Compose 页面。
    override val options = listOf(
        StringOption(
            key = "toast_text",
            title = "Toast text",
            description = "Text shown whenever an Activity is created.",
            default = "Hello from ${ModuleConfig.NAME}",
        ),
    )

    override fun install(context: HookContext) {
        val onCreate = Activity::class.java.getDeclaredMethod("onCreate", Bundle::class.java)
        val toastText = context.config.getString(
            optionKey(metadata.id, "toast_text"),
            "Hello from ${ModuleConfig.NAME}",
        )
        context.hook(onCreate) {
            after {
                val activity = thisObject as Activity
                context.bridge.log(
                    android.util.Log.INFO,
                    ModuleConfig.LOG_TAG,
                    "Activity.onCreate callback reached: ${activity.javaClass.name}",
                )
                Toast.makeText(activity, toastText, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
