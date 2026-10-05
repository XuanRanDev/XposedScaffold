package dev.xuanran.xposed.startup

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import dev.xuanran.xposed.api.HookBridge
import dev.xuanran.xposed.api.HookCallback
import dev.xuanran.xposed.api.HookFeature
import dev.xuanran.xposed.api.HookParam
import dev.xuanran.xposed.api.HostEnvironment
import dev.xuanran.xposed.api.ModuleConfig
import dev.xuanran.xposed.dexkit.DexKitResolver
import dev.xuanran.xposed.runtime.HookRegistry
import dev.xuanran.xposed.runtime.HookRuntime
import dev.xuanran.xposed.runtime.InMemoryErrorStore
import dev.xuanran.xposed.runtime.SharedPreferencesHookConfig

/**
 * Legacy 与 Modern 入口共用的启动协调器。
 *
 * 入口收到的回调通常早于 Application 创建。这里先 Hook Application.attach，等宿主 Context 与
 * ClassLoader 可用后再初始化功能，避免在 Zygote/包加载早期触发 AndroidX、DexKit 或配置系统。
 */
object ModuleStartup {
    // 每个宿主进程都有独立的模块 ClassLoader 和静态字段，因此这里的状态不会跨进程串扰。
    // installed 防止重复安装 attach Hook；started 防止同一进程重复初始化功能。
    private var installed = false
    private var started = false

    @Synchronized
    fun install(
        packageName: String,
        processName: String,
        classLoader: ClassLoader,
        bridge: HookBridge,
    ) {
        // 某些框架/应用生命周期可能重复派发包回调，入口必须保持幂等。
        if (installed) return
        installed = true
        bridge.log(android.util.Log.INFO, ModuleConfig.LOG_TAG, "Installing Application.attach hook")
        // Application.attach 在绝大多数常规应用中早于 onCreate，且已经持有可用 base Context。
        val attach = Application::class.java.getDeclaredMethod("attach", Context::class.java)
        bridge.hook(attach, callback = object : HookCallback {
            override fun after(param: HookParam) {
                val application = param.thisObject as? Application ?: return
                // 使用 after：此时 baseContext 已写入 Application，系统服务和包信息均可安全访问。
                bridge.log(android.util.Log.INFO, ModuleConfig.LOG_TAG, "Application attached; starting hook runtime")
                try {
                    start(application, packageName, processName, classLoader, bridge)
                } catch (throwable: Throwable) {
                    bridge.log(android.util.Log.ERROR, ModuleConfig.LOG_TAG, "Hook runtime startup failed", throwable)
                }
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
        // 宿主版本参与 DexKit 缓存键和 Hook 的版本范围判断，不能使用模块自身版本。
        val versionCode = application.packageManager.getPackageInfo(packageName, 0).longVersionCode
        // 注册表位于模块 APK，因此必须用模块 ClassLoader 加载，不能使用宿主 ClassLoader。
        val features = loadGeneratedHooks()
        bridge.log(android.util.Log.INFO, ModuleConfig.LOG_TAG, "Loaded ${features.size} generated hook(s)")
        HookRegistry.register(features)
        val preferences = bridge.remotePreferences(ModuleConfig.PREFERENCES_NAME)
            // 后备配置也允许 Legacy 使用者自行向宿主进程注入配置页面；标准生产路径仍是远程偏好。
            ?: application.getSharedPreferences(ModuleConfig.PREFERENCES_NAME, Context.MODE_PRIVATE)
        val config = SharedPreferencesHookConfig(preferences)
        val dex = DexKitResolver(
            // 扫描目标必须是宿主 APK；传入模块 APK 会导致所有匹配都为空。
            apkPath = application.applicationInfo.sourceDir,
            hostVersion = versionCode,
            classLoader = classLoader,
            cache = application.getSharedPreferences(ModuleConfig.DEX_PREFERENCES_NAME, Context.MODE_PRIVATE),
        )
        HookRuntime.initialize(
            HostEnvironment(packageName, processName, versionCode, classLoader, application, bridge),
            config,
            dex,
            InMemoryErrorStore,
        )
        bridge.log(android.util.Log.INFO, ModuleConfig.LOG_TAG, "Hook runtime initialized")
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadGeneratedHooks(): List<HookFeature> {
        // ModuleStartup 类本身来自模块 APK，因此它的 ClassLoader 是定位 KSP 产物最稳定的锚点。
        val moduleClassLoader = checkNotNull(ModuleStartup::class.java.classLoader)
        val registry = moduleClassLoader.loadClass("dev.xuanran.xposed.generated.GeneratedHookRegistryKt")
        // 反射只发生一次，用来切断 startup -> app 的编译期循环依赖。
        return registry.getMethod("createHooks").invoke(null) as List<HookFeature>
    }
}
