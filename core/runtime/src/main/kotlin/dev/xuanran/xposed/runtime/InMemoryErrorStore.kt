package dev.xuanran.xposed.runtime

import dev.xuanran.xposed.api.ErrorReporter
import dev.xuanran.xposed.api.HookFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object InMemoryErrorStore : ErrorReporter {
    private val mutableErrors = MutableStateFlow<List<HookFailure>>(emptyList())
    val errors: StateFlow<List<HookFailure>> = mutableErrors
    override fun report(failure: HookFailure) { mutableErrors.value = listOf(failure) + mutableErrors.value.take(99) }
    fun clear() { mutableErrors.value = emptyList() }
}
