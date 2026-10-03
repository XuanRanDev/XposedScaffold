package dev.xuanran.xposed.loader.legacy

import android.content.SharedPreferences
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import dev.xuanran.xposed.api.HookBridge
import dev.xuanran.xposed.api.HookCallback
import dev.xuanran.xposed.api.HookParam
import dev.xuanran.xposed.api.UnhookHandle
import java.lang.reflect.Executable

/** 把经典 XposedBridge/XC_MethodHook 适配为业务层统一 HookBridge。 */
object LegacyHookBridge : HookBridge {
    override val frameworkName = "Legacy Xposed"
    override val frameworkVersion get() = XposedBridge.getXposedVersion().toString()
    override val apiVersion get() = XposedBridge.getXposedVersion()

    override fun hook(executable: Executable, priority: Int, callback: HookCallback): UnhookHandle {
        // before/after 使用同一业务接口，但每次回调包装当前 MethodHookParam，避免跨调用保存状态。
        val unhook = XposedBridge.hookMethod(executable, object : XC_MethodHook(priority) {
            override fun beforeHookedMethod(param: MethodHookParam) = callback.before(LegacyParam(executable, param))
            override fun afterHookedMethod(param: MethodHookParam) = callback.after(LegacyParam(executable, param))
        })
        return UnhookHandle { unhook.unhook() }
    }

    override fun log(priority: Int, tag: String, message: String, throwable: Throwable?) {
        XposedBridge.log("[$tag] $message")
        throwable?.let(XposedBridge::log)
    }

    override fun remotePreferences(name: String): SharedPreferences =
        // XSharedPreferences 自带只读刷新语义，模块 UI 写入后宿主重启即可读取新值。
        XSharedPreferences("dev.xuanran.xposedscaffold", name).apply { reload() }
}

/** XC_MethodHook.MethodHookParam 的零拷贝视图。 */
private class LegacyParam(
    override val executable: Executable,
    private val delegate: XC_MethodHook.MethodHookParam,
) : HookParam {
    override val thisObject get() = delegate.thisObject
    @Suppress("UNCHECKED_CAST")
    override val args get() = delegate.args as Array<Any?>
    override var result: Any?
        get() = delegate.result
        set(value) { delegate.result = value }
    override var throwable: Throwable?
        get() = delegate.throwable
        set(value) { delegate.throwable = value }
    override val hasResult get() = delegate.hasThrowable() || delegate.result != null
}
