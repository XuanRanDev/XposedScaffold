package dev.xuanran.xposedscaffold

import android.app.Application
import dev.xuanran.xposed.api.ModuleConfig
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

/** Modern settings process: writes directly to LSPosed's Remote Preferences database. */
class ModuleApplication : Application(), XposedServiceHelper.OnServiceListener {
    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        ModulePreferences.preferences = service.getRemotePreferences(ModuleConfig.PREFERENCES_NAME)
        ModulePreferences.status = "Remote Preferences 已连接"
    }

    override fun onServiceDied(service: XposedService) {
        ModulePreferences.preferences = null
        ModulePreferences.status = "Xposed 服务连接已断开"
    }
}
