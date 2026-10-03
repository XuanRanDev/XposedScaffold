package dev.xuanran.xposed.loader.modern

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import dev.xuanran.xposed.startup.ModuleStartup

/**
 * libxposed API 101–102 入口。
 * 框架会先 attachFramework，再调用 onModuleLoaded，因此构造阶段不能访问 framework 属性。
 */
class ModernXposedEntry : XposedModule() {
    private var processName: String = ""
    private val bridge by lazy { ModernHookBridge(this) }

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        processName = param.processName
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        // PackageReady 能拿到最终 ClassLoader，比 PackageLoaded 更适合存在 AppComponentFactory 的宿主。
        if (param.packageName != "com.example.target") return
        ModuleStartup.install(param.packageName, processName, param.classLoader, bridge)
    }
}
