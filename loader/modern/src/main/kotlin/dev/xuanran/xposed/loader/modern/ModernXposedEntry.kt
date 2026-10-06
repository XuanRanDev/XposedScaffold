package dev.xuanran.xposed.loader.modern

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import dev.xuanran.xposed.startup.ModuleStartup
import dev.xuanran.xposed.startup.HostConfigProvider
import dev.xuanran.xposed.api.ModuleConfig
import dev.xuanran.xposed.runtime.SharedPreferencesHookConfig

/**
 * libxposed API 101–102 入口。
 * 框架会先 attachFramework，再调用 onModuleLoaded，因此构造阶段不能访问 framework 属性。
 */
class ModernXposedEntry : XposedModule() {
    private var processName: String = ""
    private val bridge by lazy { ModernHookBridge(this) }
    // Framework scope can be broader than the template configuration. Always enforce our own
    // allowlist before touching the target ClassLoader or initializing DexKit.
    private val targetPackages = BuildConfig.XPOSED_TARGET_PACKAGES
        .split(',')
        .filter(String::isNotBlank)
        .toSet()

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        processName = param.processName
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        // PackageReady 能拿到最终 ClassLoader，比 PackageLoaded 更适合存在 AppComponentFactory 的宿主。
        // This is the module-level filter; HookRuntime applies the feature-level package filter.
        if (param.packageName !in targetPackages) return
        ModuleStartup.install(
            param.packageName,
            processName,
            param.classLoader,
            bridge,
            HostConfigProvider {
                SharedPreferencesHookConfig(getRemotePreferences(ModuleConfig.PREFERENCES_NAME))
            },
        )
    }
}
