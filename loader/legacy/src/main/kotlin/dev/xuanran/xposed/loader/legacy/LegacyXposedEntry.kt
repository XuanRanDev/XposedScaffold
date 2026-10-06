package dev.xuanran.xposed.loader.legacy

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.callbacks.XC_LoadPackage
import dev.xuanran.xposed.startup.ModuleStartup
import dev.xuanran.xposed.startup.HostConfigProvider
import dev.xuanran.xposed.api.ModuleConfig
import dev.xuanran.xposed.runtime.SharedPreferencesHookConfig

/** 传统 Xposed API 82 入口；此类应保持极小，禁止放入业务功能。 */
class LegacyXposedEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(param: XC_LoadPackage.LoadPackageParam) {
        if (!TargetScope.contains(param.packageName)) return
        val process = param.processName ?: param.packageName
        try {
            ModuleStartup.install(
                param.packageName,
                process,
                param.classLoader,
                LegacyHookBridge,
                HostConfigProvider {
                    XSharedPreferences(ModuleConfig.PACKAGE, ModuleConfig.PREFERENCES_NAME)
                        .apply { reload() }
                        .takeIf { it.file.canRead() }
                        ?.let(::SharedPreferencesHookConfig)
                },
            )
        } catch (throwable: Throwable) {
            XposedBridge.log(throwable)
        }
    }
}

object TargetScope {
    // 传统 API 没有 scope.list 回调过滤，必须在入口主动筛选。
    // 列表由根 gradle.properties 生成，避免 Legacy、Modern 和设置页各维护一份包名。
    private val packages = BuildConfig.XPOSED_TARGET_PACKAGES
        .split(',')
        .filter(String::isNotBlank)
        .toSet()
    // 这里是模块级粗过滤；进入 HookRuntime 后还会按每个 @HookItem.targetPackages 精确过滤。
    fun contains(packageName: String) = packageName in packages
}
