package dev.xuanran.xposed.api

/**
 * 声明一个可被脚手架发现并展示的功能。
 *
 * KSP 在编译期生成直接对象引用，不进行运行时 DEX 扫描。被标记的类必须是 Kotlin `object`
 * 且实现 [HookFeature]。注解保留到运行时，以便标准 UI 直接读取展示信息。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HookItem(
    /** 全局唯一且稳定的功能 ID，同时作为配置命名空间。 */
    val id: String,
    /** 使用 `/` 分隔的设置页分类路径。 */
    val path: String,
    val title: String,
    val description: String = "",
    val keywords: Array<String> = [],
    val type: HookUiType = HookUiType.SWITCH,
    val targetPackages: Array<String> = [],
    val targetProcesses: Array<String> = ["main"],
    val minHostVersion: Long = 0,
    val maxHostVersion: Long = Long.MAX_VALUE,
    val restartPolicy: RestartPolicy = RestartPolicy.HOST,
    val experimental: Boolean = false,
)
