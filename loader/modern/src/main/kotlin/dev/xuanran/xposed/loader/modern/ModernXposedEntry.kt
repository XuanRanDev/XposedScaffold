package dev.xuanran.xposed.loader.modern

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import dev.xuanran.xposed.startup.ModuleStartup

class ModernXposedEntry : XposedModule() {
    private var processName: String = ""
    private val bridge by lazy { ModernHookBridge(this) }

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        processName = param.processName
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        if (param.packageName != "com.example.target") return
        ModuleStartup.install(param.packageName, processName, param.classLoader, bridge)
    }
}
