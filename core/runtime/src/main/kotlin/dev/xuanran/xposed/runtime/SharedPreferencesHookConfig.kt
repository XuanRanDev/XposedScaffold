package dev.xuanran.xposed.runtime

import android.content.SharedPreferences
import dev.xuanran.xposed.api.HookConfig

/** SharedPreferences 到稳定 HookConfig 接口的轻量适配器。 */
class SharedPreferencesHookConfig(private val preferences: SharedPreferences) : HookConfig {
    override fun getBoolean(key: String, default: Boolean) = preferences.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) { preferences.edit().putBoolean(key, value).apply() }
    override fun getString(key: String, default: String) = preferences.getString(key, default) ?: default
    override fun putString(key: String, value: String) { preferences.edit().putString(key, value).apply() }
    override fun getInt(key: String, default: Int) = preferences.getInt(key, default)
    override fun putInt(key: String, value: Int) { preferences.edit().putInt(key, value).apply() }
}

/** 所有配置均使用 Hook ID 隔离，避免不同功能的短键名互相覆盖。 */
fun enabledKey(id: String) = "hook.$id.enabled"
fun optionKey(id: String, key: String) = "hook.$id.option.$key"
