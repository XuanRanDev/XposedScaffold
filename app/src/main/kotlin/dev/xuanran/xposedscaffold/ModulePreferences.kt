package dev.xuanran.xposedscaffold

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Preference handle exposed by the active Xposed flavor to the settings UI. */
object ModulePreferences {
    var preferences: SharedPreferences? by mutableStateOf(null)
        internal set
    var status: String by mutableStateOf("正在连接 Xposed 服务")
        internal set
}
