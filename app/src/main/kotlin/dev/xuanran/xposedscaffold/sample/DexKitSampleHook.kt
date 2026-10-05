package dev.xuanran.xposedscaffold.sample

import dev.xuanran.xposed.api.HookContext
import dev.xuanran.xposed.api.HookItem
import dev.xuanran.xposed.api.ModuleConfig
import dev.xuanran.xposed.api.RestartPolicy
import dev.xuanran.xposed.api.hook
import dev.xuanran.xposed.dexkit.DexKitFeature
import dev.xuanran.xposed.dexkit.MethodTarget
import dev.xuanran.xposed.runtime.SwitchHook
import org.luckypray.dexkit.wrap.DexMethod

/**
 * DexKit 使用示例。
 *
 * 这是一个观察型 Hook，只记录目标方法的调用，不修改参数或返回值。将下面两个示例字符串
 * 替换为目标方法实际使用的、足够独特且跨版本相对稳定的字符串后即可用于真实目标。
 */
@HookItem(
    id = "sample.dexkit_method_trace",
    path = "Samples/DexKit",
    title = "DexKit 方法跟踪示例",
    description = "用方法内字符串定位混淆方法，并在调用后记录日志。",
    keywords = ["sample", "dexkit", "method", "trace"],
    targetPackages = ["com.qzone"],
    restartPolicy = RestartPolicy.HOST,
    experimental = true,
)
object DexKitSampleHook : SwitchHook(), DexKitFeature {
    // 当前 resolver 的进程内索引使用 target key；加入 Hook ID 前缀可避免跨功能冲突。
    private const val TARGET_METHOD = "sample.dexkit_method_trace.target_method"

    override val dexKitTargets = listOf(
        MethodTarget(
            key = TARGET_METHOD,
            // 修改下面的匹配条件时递增 revision，使旧规则产生的缓存立即失效。
            revision = 1,
            finder = {
                findMethod {
                    // 先限制包范围，避免遍历和误匹配无关三方库。
                    searchPackages("com.qzone")
                    matcher {
                        // 示例条件必须替换成真实目标中的稳定特征。
                        usingStrings("replace_with_unique_string_1", "replace_with_unique_string_2")
                    }
                }.single().descriptor
            },
            resolver = { classLoader, descriptor ->
                // 缓存只保存可移植的 Dalvik 描述符；每次启动都用当前宿主 ClassLoader 恢复并校验。
                DexMethod(descriptor).getMethodInstance(classLoader).apply { isAccessible = true }
            },
        ),
    )

    override fun install(context: HookContext) {
        val target = context.dex.method(TARGET_METHOD)
        context.hook(target) {
            after {
                context.bridge.log(
                    android.util.Log.INFO,
                    ModuleConfig.LOG_TAG,
                    "DexKit sample target called: $executable, result=$result",
                )
            }
        }
    }
}
