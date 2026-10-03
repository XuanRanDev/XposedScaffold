package dev.xuanran.xposed.api

import java.lang.reflect.Executable
import android.content.SharedPreferences

interface HookBridge {
    val frameworkName: String
    val frameworkVersion: String
    val apiVersion: Int

    fun hook(executable: Executable, priority: Int = 50, callback: HookCallback): UnhookHandle
    fun log(priority: Int, tag: String, message: String, throwable: Throwable? = null)
    fun remotePreferences(name: String): SharedPreferences? = null
}

fun interface UnhookHandle {
    fun unhook()
}

interface HookCallback {
    fun before(param: HookParam) = Unit
    fun after(param: HookParam) = Unit
}

interface HookParam {
    val executable: Executable
    val thisObject: Any?
    val args: Array<Any?>
    var result: Any?
    var throwable: Throwable?
    val hasResult: Boolean
}
