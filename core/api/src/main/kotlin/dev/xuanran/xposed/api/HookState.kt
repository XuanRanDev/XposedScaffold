package dev.xuanran.xposed.api

/** 一个功能从注册到运行的可观察状态。 */
sealed interface HookState {
    data object Disabled : HookState
    data object Waiting : HookState
    data object Initializing : HookState
    data object Active : HookState
    data class Unsupported(val reason: String) : HookState
    data class Failed(val stage: HookStage, val throwable: Throwable) : HookState
}

/** 用于定位错误发生在哪个生命周期阶段。 */
enum class HookStage { AVAILABILITY, DEX_RESOLUTION, INSTALL, CALLBACK, UNLOAD }

/** 可被 UI、日志导出或问题报告消费的结构化错误。 */
data class HookFailure(
    val hookId: String,
    val stage: HookStage,
    val packageName: String,
    val processName: String,
    val hostVersion: Long,
    val timestamp: Long,
    val throwable: Throwable,
)
