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
        override fun before(param: HookParam) { spec.before?.invoke(param) }
        override fun after(param: HookParam) { spec.after?.invoke(param) }
    })
}
