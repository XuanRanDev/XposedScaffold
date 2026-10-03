package dev.xuanran.xposed.dexkit

import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Field
import java.lang.reflect.Method

interface DexKitFeature {
    val dexKitTargets: List<DexKitTarget<out Any>>
}

abstract class DexKitTarget<T : Any>(
    val key: String,
    val revision: Int = 1,
) {
    abstract fun find(bridge: DexKitBridge): String
    abstract fun resolve(classLoader: ClassLoader, descriptor: String): T
}

class MethodTarget(
    key: String,
    revision: Int = 1,
    private val finder: DexKitBridge.() -> String,
    private val resolver: (ClassLoader, String) -> Method,
) : DexKitTarget<Method>(key, revision) {
    override fun find(bridge: DexKitBridge) = bridge.finder()
    override fun resolve(classLoader: ClassLoader, descriptor: String) = resolver(classLoader, descriptor)
}

class FieldTarget(
    key: String,
    revision: Int = 1,
    private val finder: DexKitBridge.() -> String,
    private val resolver: (ClassLoader, String) -> Field,
) : DexKitTarget<Field>(key, revision) {
    override fun find(bridge: DexKitBridge) = bridge.finder()
    override fun resolve(classLoader: ClassLoader, descriptor: String) = resolver(classLoader, descriptor)
}
