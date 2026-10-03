package dev.xuanran.xposed.api

enum class HookUiType { SWITCH, ACTION, API }
enum class RestartPolicy { NONE, HOST, DEVICE }

data class HookMetadata(
    val id: String,
    val path: List<String>,
    val title: String,
    val description: String,
    val keywords: Set<String>,
    val uiType: HookUiType,
    val targetPackages: Set<String>,
    val targetProcesses: Set<String>,
    val minHostVersion: Long,
    val maxHostVersion: Long,
    val restartPolicy: RestartPolicy,
    val experimental: Boolean,
)
