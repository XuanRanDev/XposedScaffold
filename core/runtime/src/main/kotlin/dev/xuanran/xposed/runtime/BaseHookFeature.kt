package dev.xuanran.xposed.runtime

import dev.xuanran.xposed.api.HookFeature
import dev.xuanran.xposed.api.HookItem
import dev.xuanran.xposed.api.HookMetadata

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
            uiType = item.type,
            targetPackages = item.targetPackages.toSet(),
            targetProcesses = item.targetProcesses.toSet(),
            minHostVersion = item.minHostVersion,
            maxHostVersion = item.maxHostVersion,
            restartPolicy = item.restartPolicy,
            experimental = item.experimental,
        )
    }
}

abstract class SwitchHook : BaseHookFeature()
abstract class ApiHook : BaseHookFeature()

abstract class ActionHook : BaseHookFeature() {
    final override fun install(context: dev.xuanran.xposed.api.HookContext) = Unit
    abstract fun run(context: android.content.Context)
}
