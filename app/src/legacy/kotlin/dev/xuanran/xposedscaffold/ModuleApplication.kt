package dev.xuanran.xposedscaffold

import android.app.Application
import android.content.Context
import dev.xuanran.xposed.api.ModuleConfig

/** Legacy settings process using LSPosed's New XSharedPreferences extension. */
class ModuleApplication : Application() {
    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()
        try {
            // MODE_WORLD_READABLE 在普通 Android 应用中已废弃，但 LSPosed 开启
            // xposedsharedprefs 后会拦截并安全地把文件放到框架管理的随机目录。
            ModulePreferences.preferences = getSharedPreferences(
                ModuleConfig.PREFERENCES_NAME,
                Context.MODE_WORLD_READABLE,
            )
            ModulePreferences.status = "New XSharedPreferences 已启用"
        } catch (_: SecurityException) {
            // 未安装兼容 LSPosed、模块未启用或框架不支持 New XSharedPreferences 时会到这里。
            // 不创建假的本地配置，避免用户看到开关已保存但宿主永远读取不到。
            ModulePreferences.preferences = null
            ModulePreferences.status = "当前框架不支持 New XSharedPreferences"
        }
    }
}
