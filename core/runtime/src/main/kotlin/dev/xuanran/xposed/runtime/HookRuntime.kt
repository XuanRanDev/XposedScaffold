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
            if (metadata.targetPackages.isNotEmpty() && environment.packageName !in metadata.targetPackages) return@forEach
            if (!matchesProcess(metadata.targetProcesses, environment)) return@forEach
            if (environment.versionCode !in metadata.minHostVersion..metadata.maxHostVersion) {
                record.update(HookState.Unsupported("Host version ${environment.versionCode} is outside the supported range"))
                return@forEach
            }
            val availability = feature.isAvailable(environment)
            if (availability is Availability.Unavailable) {
                record.update(HookState.Unsupported(availability.reason))
                return@forEach
            }
            if (metadata.uiType == HookUiType.ACTION) {
                record.update(HookState.Disabled)
                return@forEach
            }
            val enabled = metadata.uiType == HookUiType.API || config.getBoolean(enabledKey(metadata.id), false)
            if (!enabled) {
                record.update(HookState.Disabled)
                return@forEach
            }
            record.update(HookState.Initializing)
            try {
                if (!dex.resolve(feature)) {
                    record.update(HookState.Unsupported("DexKit targets are not ready"))
                    return@forEach
                }
                feature.install(HookContext(environment, dex, config, errors))
                record.update(HookState.Active)
            } catch (throwable: Throwable) {
                val failure = HookFailure(
                    metadata.id, HookStage.INSTALL, environment.packageName, environment.processName,
                    environment.versionCode, System.currentTimeMillis(), throwable,
                )
                errors.report(failure)
                record.update(HookState.Failed(HookStage.INSTALL, throwable))
            }
        }
    }

    private fun matchesProcess(targets: Set<String>, environment: HostEnvironment): Boolean =
        "*" in targets || "any" in targets || environment.processName in targets ||
            ("main" in targets && environment.processName == environment.packageName)
}
