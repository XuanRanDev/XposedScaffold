package dev.xuanran.xposed.api

import android.content.Context

/**
 * 当前被注入宿主的稳定环境快照。
 *
 * 注意 [applicationContext] 属于宿主而不是模块自身，不能据此访问模块私有文件。
 */
data class HostEnvironment(
    val packageName: String,
    val processName: String,
    val versionCode: Long,
    val classLoader: ClassLoader,
    val applicationContext: Context,
    val hookBridge: HookBridge,
)
