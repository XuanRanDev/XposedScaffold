package dev.xuanran.xposed.api

import java.lang.reflect.Executable

class HookBuilder {
    internal var before: (HookParam.() -> Unit)? = null
    internal var after: (HookParam.() -> Unit)? = null

    fun before(block: HookParam.() -> Unit) { before = block }
    fun after(block: HookParam.() -> Unit) { after = block }
}

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
