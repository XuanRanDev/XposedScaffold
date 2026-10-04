package dev.xuanran.xposed.api

import java.lang.reflect.Executable

/** 暂存 DSL 中声明的 before/after 回调。 */
class HookBuilder {
    internal var before: (HookParam.() -> Unit)? = null
    internal var after: (HookParam.() -> Unit)? = null

    fun before(block: HookParam.() -> Unit) { before = block }
    fun after(block: HookParam.() -> Unit) { after = block }
}

/**
 * 安装一个与框架无关的 Hook。
 *
 * 示例：`context.hook(method) { before { args[0] = "new value" } }`。
 */
fun HookContext.hook(
    executable: Executable,
    priority: Int = 50,
    block: HookBuilder.() -> Unit,
): UnhookHandle {
    val spec = HookBuilder().apply(block)
    return bridge.hook(executable, priority, object : HookCallback {
        override fun before(param: HookParam) { runSafely("before", param, spec.before) }
        override fun after(param: HookParam) { runSafely("after", param, spec.after) }

        private fun runSafely(
            phase: String,
            param: HookParam,
            callback: (HookParam.() -> Unit)?,
        ) {
            if (callback == null) return
            try {
                callback(param)
            } catch (throwable: Throwable) {
                errors.report(
                    HookFailure(
                        hookId = hookId,
                        stage = HookStage.CALLBACK,
                        packageName = environment.packageName,
                        processName = environment.processName,
                        hostVersion = environment.versionCode,
                        timestamp = System.currentTimeMillis(),
                        throwable = throwable,
                    ),
                )
                bridge.log(
                    android.util.Log.ERROR,
                    ModuleConfig.LOG_TAG,
                    "Hook callback failed: $hookId $phase ${param.executable}",
                    throwable,
                )
            }
        }
    })
}
