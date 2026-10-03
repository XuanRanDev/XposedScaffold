package dev.xuanran.xposed.loader.legacy

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import dev.xuanran.xposed.startup.ModuleStartup

class LegacyXposedEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(param: XC_LoadPackage.LoadPackageParam) {
        if (!TargetScope.contains(param.packageName)) return
        ModuleStartup.install(param.packageName, param.processName, param.classLoader, LegacyHookBridge)
    }
}

object TargetScope {
    // Keep this list synchronized with app/src/main/resources/META-INF/xposed/scope.list.
    private val packages = setOf("com.example.target")
    fun contains(packageName: String) = packageName in packages
}
