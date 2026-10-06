package dev.xuanran.xposedscaffold

import android.content.Context
import androidx.lifecycle.ViewModel
import dev.xuanran.xposed.api.HookOption
import dev.xuanran.xposed.runtime.ActionHook
import dev.xuanran.xposed.runtime.HookRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActionFeedback(val title: String, val error: Throwable? = null)

class ModuleViewModel : ViewModel() {
    val uiState = ModuleRepository.state
    private val mutableActionFeedback = MutableStateFlow<ActionFeedback?>(null)
    val actionFeedback = mutableActionFeedback.asStateFlow()

    fun setEnabled(hookId: String, enabled: Boolean) = ModuleRepository.setEnabled(hookId, enabled)
    fun setBoolean(hookId: String, option: HookOption, value: Boolean) =
        ModuleRepository.setBoolean(hookId, option, value)
    fun setString(hookId: String, option: HookOption, value: String) =
        ModuleRepository.setString(hookId, option, value)
    fun setInt(hookId: String, option: HookOption, value: Int) =
        ModuleRepository.setInt(hookId, option, value)

    fun runAction(context: Context, record: HookRecord) {
        val action = record.feature as? ActionHook ?: return
        mutableActionFeedback.value = runCatching { action.run(context) }
            .fold(
                onSuccess = { ActionFeedback(record.feature.metadata.title) },
                onFailure = { ActionFeedback(record.feature.metadata.title, it) },
            )
    }

    fun consumeActionFeedback() { mutableActionFeedback.value = null }
}
