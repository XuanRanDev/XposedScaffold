package dev.xuanran.xposed.loader.modern

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import dev.xuanran.xposed.api.HookBridge
import dev.xuanran.xposed.api.HookCallback
import dev.xuanran.xposed.api.HookParam
import dev.xuanran.xposed.api.UnhookHandle
import java.lang.reflect.Executable

/** 把 libxposed 的拦截器链模型适配成统一的 before/after 语义。 */
class ModernHookBridge(private val module: XposedModule) : HookBridge {
    override val frameworkName get() = module.frameworkName
    override val frameworkVersion get() = module.frameworkVersion
    override val apiVersion get() = module.apiVersion

    override fun hook(executable: Executable, priority: Int, callback: HookCallback): UnhookHandle {
        val handle = module.hook(executable)
            .setPriority(priority)
            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
            .intercept(ModernHooker(callback))
        return UnhookHandle(handle::unhook)
    }

    override fun log(priority: Int, tag: String, message: String, throwable: Throwable?) {
        if (throwable == null) module.log(priority, tag, message)
        else module.log(priority, tag, message, throwable)
    }
}

/** Stable, named framework callback boundary; kept explicitly from R8 in the app rules. */
internal class ModernHooker(private val callback: HookCallback) : XposedInterface.Hooker {
    override fun intercept(chain: XposedInterface.Chain): Any? {
        val param = ModernParam(chain)
        callback.before(param)
        // before 未提前给出结果/异常时才继续执行链，修改后的 args 会传给后续 Hook。
        if (!param.hasResult) {
            try {
                param.result = chain.proceed(param.args)
            } catch (throwable: Throwable) {
                param.throwable = throwable
            }
        }
        callback.after(param)
        // after 最终决定向原调用者返回结果还是抛出异常。
        param.throwable?.let { throw it }
        return param.result
    }
}

/**
 * Chain 的可变代理。API 102 的参数是只读 List，因此复制为数组供传统风格 DSL 修改。
 */
private class ModernParam(private val chain: XposedInterface.Chain) : HookParam {
    override val executable get() = chain.executable
    override val thisObject get() = chain.thisObject
    @Suppress("UNCHECKED_CAST")
    override val args = chain.args.toTypedArray()
    private var completed = false
    override var result: Any? = null
        set(value) { field = value; completed = true; throwable = null }
    override var throwable: Throwable? = null
        set(value) { field = value; if (value != null) completed = true }
    override val hasResult get() = completed
}
