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
            ModulePreferences.preferences = getSharedPreferences(
                ModuleConfig.PREFERENCES_NAME,
                Context.MODE_WORLD_READABLE,
            )
            ModulePreferences.status = "New XSharedPreferences 已启用"
        } catch (_: SecurityException) {
            ModulePreferences.preferences = null
            ModulePreferences.status = "当前框架不支持 New XSharedPreferences"
        }
    }
}
