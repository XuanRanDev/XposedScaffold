package dev.xuanran.xposedscaffold

import android.content.SharedPreferences
import dev.xuanran.xposed.api.HookOption
import dev.xuanran.xposed.runtime.enabledKey
import dev.xuanran.xposed.runtime.optionKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ConnectionState { CONNECTING, LEGACY_CONNECTED, MODERN_CONNECTED, DISCONNECTED, UNSUPPORTED }

data class ModuleUiState(
    val preferences: SharedPreferences? = null,
    val connection: ConnectionState = ConnectionState.CONNECTING,
    val preferenceRevision: Long = 0,
)

/** Single settings data source shared by the flavor adapters and UI state holder. */
object ModuleRepository : SharedPreferences.OnSharedPreferenceChangeListener {
    private val mutableState = MutableStateFlow(ModuleUiState())
    val state = mutableState.asStateFlow()

    fun connect(preferences: SharedPreferences, connection: ConnectionState) {
        mutableState.value.preferences?.unregisterOnSharedPreferenceChangeListener(this)
        preferences.registerOnSharedPreferenceChangeListener(this)
        mutableState.value = ModuleUiState(preferences, connection)
    }

    fun disconnect(connection: ConnectionState) {
        mutableState.value.preferences?.unregisterOnSharedPreferenceChangeListener(this)
        mutableState.value = ModuleUiState(connection = connection)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        mutableState.update { it.copy(preferenceRevision = it.preferenceRevision + 1) }
    }

    fun setEnabled(hookId: String, enabled: Boolean) = edit { putBoolean(enabledKey(hookId), enabled) }
    fun setBoolean(hookId: String, option: HookOption, value: Boolean) =
        edit { putBoolean(optionKey(hookId, option.key), value) }
    fun setString(hookId: String, option: HookOption, value: String) =
        edit { putString(optionKey(hookId, option.key), value) }
    fun setInt(hookId: String, option: HookOption, value: Int) =
        edit { putInt(optionKey(hookId, option.key), value) }

    private inline fun edit(block: SharedPreferences.Editor.() -> Unit) {
        state.value.preferences?.edit()?.apply(block)?.apply()
    }
}
