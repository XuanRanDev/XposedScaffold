package dev.xuanran.xposed.api

/** 设置页自动选择的标准功能表现形式。 */
enum class HookUiType { SWITCH, ACTION, API }
/** 修改配置后需要怎样才能完整生效。 */
enum class RestartPolicy { NONE, HOST, DEVICE }

/**
 * Runtime 与 UI 共享的不可变功能元数据。
 * 此模型不引用 Compose，因此核心模块可以脱离 UI 独立复用和测试。
 */
data class HookMetadata(
    /** 永久稳定的配置主键，发布后不要修改。 */
    val id: String,
    /** 设置页分类层级，例如 `["Chat", "Appearance"]`。 */
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
