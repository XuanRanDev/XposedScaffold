package dev.xuanran.xposed.api

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HookItem(
    val id: String,
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
