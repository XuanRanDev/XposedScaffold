package dev.xuanran.xposed.loader.modern

import android.content.SharedPreferences
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import dev.xuanran.xposed.api.HookBridge
import dev.xuanran.xposed.api.HookCallback
import dev.xuanran.xposed.api.HookParam
import dev.xuanran.xposed.api.UnhookHandle
import java.lang.reflect.Executable

class ModernHookBridge(private val module: XposedModule) : HookBridge {
    override val frameworkName get() = module.frameworkName
    override val frameworkVersion get() = module.frameworkVersion
    override val apiVersion get() = module.apiVersion

    override fun hook(executable: Executable, priority: Int, callback: HookCallback): UnhookHandle {
        val handle = module.hook(executable)
            .setPriority(priority)
            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
            .intercept { chain ->
                val param = ModernParam(chain)
                callback.before(param)
                if (!param.hasResult) {
                    try {
                        param.result = chain.proceed(param.args)
                    } catch (throwable: Throwable) {
                        param.throwable = throwable
                    }
                }
                callback.after(param)
                param.throwable?.let { throw it }
                param.result
            }
        return UnhookHandle(handle::unhook)
    }

    override fun log(priority: Int, tag: String, message: String, throwable: Throwable?) {
        if (throwable == null) module.log(priority, tag, message)
        else module.log(priority, tag, message, throwable)
    }

    override fun remotePreferences(name: String): SharedPreferences = module.getRemotePreferences(name)
}

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
