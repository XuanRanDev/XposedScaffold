package dev.xuanran.xposed.api

import android.content.Context

data class HostEnvironment(
    val packageName: String,
    val processName: String,
    val versionCode: Long,
    val classLoader: ClassLoader,
    val applicationContext: Context,
    val hookBridge: HookBridge,
)
