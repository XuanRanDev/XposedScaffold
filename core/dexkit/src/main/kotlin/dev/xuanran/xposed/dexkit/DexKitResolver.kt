package dev.xuanran.xposed.dexkit

import android.content.SharedPreferences
import dev.xuanran.xposed.api.DexResolver
import dev.xuanran.xposed.api.HookFeature
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Field
import java.lang.reflect.Method

class DexKitResolver(
    private val apkPath: String,
    private val hostVersion: Long,
    private val classLoader: ClassLoader,
    private val cache: SharedPreferences,
) : DexResolver {
    private val resolved = mutableMapOf<String, Any>()

    override fun resolve(feature: HookFeature): Boolean {
        val dexFeature = feature as? DexKitFeature ?: return true
        val unresolved = dexFeature.dexKitTargets.filterNot { loadCached(feature.metadata.id, it) }
        if (unresolved.isEmpty()) return true
        return runCatching {
            DexKitBridge.create(apkPath).use { bridge ->
                unresolved.forEach { target ->
                    val descriptor = target.find(bridge)
                    val member = target.resolve(classLoader, descriptor)
                    resolved[target.key] = member
                    cache.edit().putString(cacheKey(feature.metadata.id, target), descriptor).apply()
                }
            }
            true
        }.getOrDefault(false)
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
        cache.edit().apply {
            cache.all.keys.filter { it.startsWith("dex.$featureId.") }.forEach(::remove)
        }.apply()
    }

    private fun cacheKey(featureId: String, target: DexKitTarget<out Any>) =
        "dex.$featureId.${target.key}.v$hostVersion.r${target.revision}"
}
