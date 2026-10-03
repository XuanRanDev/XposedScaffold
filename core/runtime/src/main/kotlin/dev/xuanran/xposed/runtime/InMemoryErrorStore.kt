package dev.xuanran.xposed.runtime

import dev.xuanran.xposed.api.ErrorReporter
import dev.xuanran.xposed.api.HookFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 默认进程内错误仓库。
 * 只保留最近 100 条以限制宿主内存占用；正式项目可替换为脱敏后的持久化实现。
 */
object InMemoryErrorStore : ErrorReporter {
    private val mutableErrors = MutableStateFlow<List<HookFailure>>(emptyList())
    val errors: StateFlow<List<HookFailure>> = mutableErrors
    override fun report(failure: HookFailure) { mutableErrors.value = listOf(failure) + mutableErrors.value.take(99) }
    fun clear() { mutableErrors.value = emptyList() }
}
