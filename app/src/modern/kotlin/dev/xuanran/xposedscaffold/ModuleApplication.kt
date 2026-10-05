package dev.xuanran.xposedscaffold

import android.app.Application
import dev.xuanran.xposed.api.ModuleConfig
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

/** Modern settings process: writes directly to LSPosed's Remote Preferences database. */
class ModuleApplication : Application(), XposedServiceHelper.OnServiceListener {
    override fun onCreate() {
        super.onCreate()
        // Service 由框架异步投递，不能假设 Activity 创建时已经可用。
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        // 设置端拿到的是可写实例；注入目标进程中的 XposedModule 拿到同组只读实例。
        ModulePreferences.preferences = service.getRemotePreferences(ModuleConfig.PREFERENCES_NAME)
        ModulePreferences.status = "Remote Preferences 已连接"
    }

    override fun onServiceDied(service: XposedService) {
        // Binder 死亡后立即清空引用，使 Compose 禁用开关，避免写入失效代理。
        ModulePreferences.preferences = null
        ModulePreferences.status = "Xposed 服务连接已断开"
    }
}
