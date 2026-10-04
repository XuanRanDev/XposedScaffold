package dev.xuanran.xposed.loader.legacy

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import dev.xuanran.xposed.startup.ModuleStartup

/** 传统 Xposed API 82 入口；此类应保持极小，禁止放入业务功能。 */
class LegacyXposedEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(param: XC_LoadPackage.LoadPackageParam) {
        if (!TargetScope.contains(param.packageName)) return
        ModuleStartup.install(param.packageName, param.processName, param.classLoader, LegacyHookBridge)
    }
}

object TargetScope {
    // 传统 API 没有 scope.list 回调过滤，必须在入口主动筛选。
    // 请与 loader/modern/src/main/resources/META-INF/xposed/scope.list 保持一致。
    private val packages = setOf("com.example.target")
    fun contains(packageName: String) = packageName in packages
}
