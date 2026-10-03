package dev.xuanran.xposed.api

sealed interface HookState {
    data object Disabled : HookState
    data object Waiting : HookState
    data object Initializing : HookState
    data object Active : HookState
    data class Unsupported(val reason: String) : HookState
    data class Failed(val stage: HookStage, val throwable: Throwable) : HookState
}

enum class HookStage { AVAILABILITY, DEX_RESOLUTION, INSTALL, CALLBACK, UNLOAD }

data class HookFailure(
    val hookId: String,
    val stage: HookStage,
    val packageName: String,
    val processName: String,
    val hostVersion: Long,
    val timestamp: Long,
    val throwable: Throwable,
)
