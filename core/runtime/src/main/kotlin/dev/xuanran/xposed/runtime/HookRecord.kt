package dev.xuanran.xposed.runtime

import dev.xuanran.xposed.api.HookFeature
import dev.xuanran.xposed.api.HookState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 把静态功能对象和可观察生命周期状态组合起来。 */
class HookRecord internal constructor(val feature: HookFeature) {
    private val mutableState = MutableStateFlow<HookState>(HookState.Waiting)
    val state: StateFlow<HookState> = mutableState
    internal fun update(state: HookState) { mutableState.value = state }
}

/**
 * 当前进程内的功能注册表。
 * KSP 负责生成输入列表，这里负责去重并为每项创建状态容器。
 */
object HookRegistry {
    private val records = linkedMapOf<String, HookRecord>()

    @Synchronized
    fun register(features: List<HookFeature>) {
        features.forEach { feature ->
            check(records.putIfAbsent(feature.metadata.id, HookRecord(feature)) == null) {
                "Duplicate hook id: ${feature.metadata.id}"
            }
        }
    }

    fun all(): List<HookRecord> = records.values.toList()
    fun find(id: String): HookRecord? = records[id]
}
