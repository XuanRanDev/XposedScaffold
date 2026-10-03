package dev.xuanran.xposed.startup

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import dev.xuanran.xposed.api.HookBridge
import dev.xuanran.xposed.api.HookCallback
import dev.xuanran.xposed.api.HookFeature
import dev.xuanran.xposed.api.HookParam
import dev.xuanran.xposed.api.HostEnvironment
import dev.xuanran.xposed.dexkit.DexKitResolver
import dev.xuanran.xposed.runtime.HookRegistry
import dev.xuanran.xposed.runtime.HookRuntime
import dev.xuanran.xposed.runtime.InMemoryErrorStore
import dev.xuanran.xposed.runtime.SharedPreferencesHookConfig

object ModuleStartup {
    private var installed = false
    private var started = false

    @Synchronized
    fun install(
        packageName: String,
        processName: String,
        classLoader: ClassLoader,
        bridge: HookBridge,
    ) {
        if (installed) return
        installed = true
        val attach = Application::class.java.getDeclaredMethod("attach", Context::class.java)
        bridge.hook(attach, callback = object : HookCallback {
            override fun after(param: HookParam) {
                val application = param.thisObject as? Application ?: return
                start(application, packageName, processName, classLoader, bridge)
            }
        })
    }

    @Synchronized
    private fun start(
        application: Application,
        packageName: String,
        processName: String,
        classLoader: ClassLoader,
        bridge: HookBridge,
    ) {
        if (started) return
        started = true
        val versionCode = application.packageManager.getPackageInfo(packageName, 0).longVersionCode
        val features = loadGeneratedHooks()
        HookRegistry.register(features)
        val preferences = bridge.remotePreferences("xposed_scaffold")
            ?: application.getSharedPreferences("xposed_scaffold", Context.MODE_PRIVATE)
        val config = SharedPreferencesHookConfig(preferences)
        val dex = DexKitResolver(
            apkPath = application.applicationInfo.sourceDir,
            hostVersion = versionCode,
            classLoader = classLoader,
            cache = application.getSharedPreferences("xposed_scaffold_dex", Context.MODE_PRIVATE),
        )
        HookRuntime.initialize(
            HostEnvironment(packageName, processName, versionCode, classLoader, application, bridge),
            config,
            dex,
            InMemoryErrorStore,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadGeneratedHooks(): List<HookFeature> {
        val moduleClassLoader = checkNotNull(ModuleStartup::class.java.classLoader)
        val registry = moduleClassLoader.loadClass("dev.xuanran.xposed.generated.GeneratedHookRegistryKt")
        return registry.getMethod("createHooks").invoke(null) as List<HookFeature>
    }
}
