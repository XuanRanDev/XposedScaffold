package dev.xuanran.xposed.dexkit

import android.content.SharedPreferences
import dev.xuanran.xposed.api.DexResolver
import dev.xuanran.xposed.api.HookFeature
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * DexKit 查询、缓存和反射恢复的统一入口。
 *
 * 缓存键同时包含功能 ID、目标 key、宿主版本和规则 revision。这样宿主升级或匹配规则变化时，
 * 旧描述符不会被误用。内存中的 [resolved] 只保存本进程已经校验成功的成员。
 */
class DexKitResolver(
    private val apkPath: String,
    private val hostVersion: Long,
    private val classLoader: ClassLoader,
    private val cache: SharedPreferences,
) : DexResolver {
    private val resolved = mutableMapOf<String, Any>()

    override fun resolve(feature: HookFeature): Boolean {
        val dexFeature = feature as? DexKitFeature ?: return true
        // 快路径：先尝试从持久化描述符恢复，正常启动无需重新扫描整个 APK。
        val unresolved = dexFeature.dexKitTargets.filterNot { loadCached(feature.metadata.id, it) }
        if (unresolved.isEmpty()) return true
        // 只有确实存在失效目标时才创建 DexKitBridge；use 确保 native 资源及时释放。
        // 查询异常必须交给 Runtime 记录，不能折叠成一个没有原因的 false。
        DexKitBridge.create(apkPath).use { bridge ->
            unresolved.forEach { target ->
                val descriptor = target.find(bridge)
                val member = target.resolve(classLoader, descriptor)
                resolved[target.key] = member
                cache.edit().putString(cacheKey(feature.metadata.id, target), descriptor).apply()
            }
        }
        return true
    }

    private fun loadCached(featureId: String, target: DexKitTarget<out Any>): Boolean {
        val descriptor = cache.getString(cacheKey(featureId, target), null) ?: return false
        return runCatching {
            resolved[target.key] = target.resolve(classLoader, descriptor)
            true
        }.getOrDefault(false)
    }

    override fun method(key: String): Method = resolved[key] as? Method
        ?: error("DexKit method target is not resolved: $key")

    override fun field(key: String): Field = resolved[key] as? Field
        ?: error("DexKit field target is not resolved: $key")

    override fun invalidate(featureId: String) {
        // 只清除指定功能，避免一个功能适配失败导致全部功能重新扫描。
        cache.edit().apply {
            cache.all.keys.filter { it.startsWith("dex.$featureId.") }.forEach(::remove)
        }.apply()
    }

    private fun cacheKey(featureId: String, target: DexKitTarget<out Any>) =
        "dex.$featureId.${target.key}.v$hostVersion.r${target.revision}"
}
