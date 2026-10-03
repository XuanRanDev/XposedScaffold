package dev.xuanran.xposed.runtime

import android.content.SharedPreferences
import dev.xuanran.xposed.api.HookConfig

class SharedPreferencesHookConfig(private val preferences: SharedPreferences) : HookConfig {
    override fun getBoolean(key: String, default: Boolean) = preferences.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) { preferences.edit().putBoolean(key, value).apply() }
    override fun getString(key: String, default: String) = preferences.getString(key, default) ?: default
    override fun putString(key: String, value: String) { preferences.edit().putString(key, value).apply() }
    override fun getInt(key: String, default: Int) = preferences.getInt(key, default)
    override fun putInt(key: String, value: Int) { preferences.edit().putInt(key, value).apply() }
}

fun enabledKey(id: String) = "hook.$id.enabled"
fun optionKey(id: String, key: String) = "hook.$id.option.$key"
