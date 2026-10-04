package dev.xuanran.xposed.runtime

import dev.xuanran.xposed.api.Availability
import dev.xuanran.xposed.api.DexResolver
import dev.xuanran.xposed.api.ErrorReporter
import dev.xuanran.xposed.api.HookConfig
import dev.xuanran.xposed.api.HookContext
import dev.xuanran.xposed.api.HookFailure
import dev.xuanran.xposed.api.HookStage
import dev.xuanran.xposed.api.HookState
import dev.xuanran.xposed.api.HookUiType
import dev.xuanran.xposed.api.HostEnvironment
import dev.xuanran.xposed.api.ModuleConfig

/**
 * 功能生命周期调度器。
 *
 * 顺序非常重要：先过滤包和进程，再检查版本与额外条件，然后解析 DexKit，最后才安装 Hook。
 * 任一功能失败都只改变自身状态，不能阻断其他功能。
 */
object HookRuntime {
    fun initialize(
        environment: HostEnvironment,
        config: HookConfig,
        dex: DexResolver,
        errors: ErrorReporter,
    ) {
        HookRegistry.all().forEach { record ->
            val feature = record.feature
            val metadata = feature.metadata
            // 尽早过滤无关环境，避免在宿主的卫星进程中创建重型对象。
            if (metadata.targetPackages.isNotEmpty() && environment.packageName !in metadata.targetPackages) {
                logSkip(environment, metadata.id, "package ${environment.packageName} is outside targetPackages")
                return@forEach
            }
            if (!matchesProcess(metadata.targetProcesses, environment.packageName, environment.processName)) {
                logSkip(environment, metadata.id, "process ${environment.processName} is outside targetProcesses")
                return@forEach
            }
            if (environment.versionCode !in metadata.minHostVersion..metadata.maxHostVersion) {
                record.update(HookState.Unsupported("Host version ${environment.versionCode} is outside the supported range"))
                logSkip(environment, metadata.id, "host version ${environment.versionCode} is unsupported")
                return@forEach
            }
            val availability = feature.isAvailable(environment)
            if (availability is Availability.Unavailable) {
                record.update(HookState.Unsupported(availability.reason))
                logSkip(environment, metadata.id, availability.reason)
                return@forEach
            }
            if (metadata.uiType == HookUiType.ACTION) {
                // ActionHook 只属于模块设置应用，不应在目标宿主中执行。
                record.update(HookState.Disabled)
                logSkip(environment, metadata.id, "action hooks only run from module settings")
                return@forEach
            }
            val enabled = metadata.uiType == HookUiType.API || config.getBoolean(enabledKey(metadata.id), false)
            if (!enabled) {
                record.update(HookState.Disabled)
                logSkip(environment, metadata.id, "feature switch is disabled")
                return@forEach
            }
            record.update(HookState.Initializing)
            try {
                if (!dex.resolve(feature)) {
                    record.update(HookState.Unsupported("DexKit targets are not ready"))
                    logSkip(environment, metadata.id, "DexKit targets are not ready")
                    return@forEach
                }
            } catch (throwable: Throwable) {
                reportFailure(environment, errors, metadata.id, HookStage.DEX_RESOLUTION, throwable)
                record.update(HookState.Failed(HookStage.DEX_RESOLUTION, throwable))
                return@forEach
            }
            try {
                feature.install(HookContext(metadata.id, environment, dex, config, errors))
                record.update(HookState.Active)
                environment.hookBridge.log(
                    android.util.Log.INFO,
                    ModuleConfig.LOG_TAG,
                    "Installed hook ${metadata.id} in ${environment.packageName}/${environment.processName}",
                )
            } catch (throwable: Throwable) {
                reportFailure(environment, errors, metadata.id, HookStage.INSTALL, throwable)
                record.update(HookState.Failed(HookStage.INSTALL, throwable))
            }
        }
    }

    internal fun matchesProcess(targets: Set<String>, packageName: String, processName: String): Boolean =
        // `main` 是逻辑别名；Android 主进程名默认等于包名。
        targets.isEmpty() || "*" in targets || "any" in targets || processName in targets ||
            ("main" in targets && processName == packageName)

    private fun logSkip(environment: HostEnvironment, hookId: String, reason: String) {
        environment.hookBridge.log(android.util.Log.INFO, ModuleConfig.LOG_TAG, "Skipped hook $hookId: $reason")
    }

    private fun reportFailure(
        environment: HostEnvironment,
        errors: ErrorReporter,
        hookId: String,
        stage: HookStage,
        throwable: Throwable,
    ) {
        errors.report(
            HookFailure(
                hookId, stage, environment.packageName, environment.processName,
                environment.versionCode, System.currentTimeMillis(), throwable,
            ),
        )
        environment.hookBridge.log(
            android.util.Log.ERROR,
            ModuleConfig.LOG_TAG,
            "Hook $hookId failed during $stage in ${environment.packageName}/${environment.processName}",
            throwable,
        )
    }
}
