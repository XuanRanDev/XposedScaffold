package dev.xuanran.xposed.runtime

import dev.xuanran.xposed.api.HookFeature
import dev.xuanran.xposed.api.HookItem
import dev.xuanran.xposed.api.HookMetadata

/**
 * 标准功能基类，负责把 @HookItem 注解转换为运行时元数据。
 * lazy 可避免在 Xposed 入口极早期进行不必要的反射和 Kotlin 初始化。
 */
abstract class BaseHookFeature : HookFeature {
    final override val metadata: HookMetadata by lazy {
        val item = javaClass.getAnnotation(HookItem::class.java)
            ?: error("${javaClass.name} must be annotated with @HookItem")
        HookMetadata(
            id = item.id,
            path = item.path.split('/').filter(String::isNotBlank),
            title = item.title,
            description = item.description,
            keywords = item.keywords.toSet(),
            uiType = when (this) {
                is ApiHook -> dev.xuanran.xposed.api.HookUiType.API
                is ActionHook -> dev.xuanran.xposed.api.HookUiType.ACTION
                else -> item.type
            },
            targetPackages = item.targetPackages.toSet(),
            targetProcesses = item.targetProcesses.toSet(),
            minHostVersion = item.minHostVersion,
            maxHostVersion = item.maxHostVersion,
            restartPolicy = item.restartPolicy,
            experimental = item.experimental,
        )
    }
}

/** 在设置页显示持久化开关的普通功能。 */
abstract class SwitchHook : BaseHookFeature()
/** 不显示用户开关、在目标环境中自动初始化的基础能力。 */
abstract class ApiHook : BaseHookFeature()

/** 在模块应用进程中点击执行一次的操作项，不会在宿主内自动安装 Hook。 */
abstract class ActionHook : BaseHookFeature() {
    final override fun install(context: dev.xuanran.xposed.api.HookContext) = Unit
    abstract fun run(context: android.content.Context)
}
