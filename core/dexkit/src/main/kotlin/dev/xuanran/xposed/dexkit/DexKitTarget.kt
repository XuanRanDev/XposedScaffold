package dev.xuanran.xposed.dexkit

import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Field
import java.lang.reflect.Method

/** 标记一个功能需要 DexKit 解析目标。普通 Hook 无需实现此接口。 */
interface DexKitFeature {
    val dexKitTargets: List<DexKitTarget<out Any>>
}

/**
 * 一个可缓存的 DexKit 查询目标。
 *
 * [find] 只负责昂贵的 DEX 搜索并返回可持久化描述符；[resolve] 负责在每次进程启动时把描述符
 * 还原为当前 ClassLoader 下的反射对象。修改查询规则时必须递增 [revision]。
 */
abstract class DexKitTarget<T : Any>(
    val key: String,
    val revision: Int = 1,
) {
    abstract fun find(bridge: DexKitBridge): String
    abstract fun resolve(classLoader: ClassLoader, descriptor: String): T
}

/** 方法目标的便捷实现。 */
class MethodTarget(
    key: String,
    revision: Int = 1,
    private val finder: DexKitBridge.() -> String,
    private val resolver: (ClassLoader, String) -> Method,
) : DexKitTarget<Method>(key, revision) {
    override fun find(bridge: DexKitBridge) = bridge.finder()
    override fun resolve(classLoader: ClassLoader, descriptor: String) = resolver(classLoader, descriptor)
}

/** 字段目标的便捷实现。 */
class FieldTarget(
    key: String,
    revision: Int = 1,
    private val finder: DexKitBridge.() -> String,
    private val resolver: (ClassLoader, String) -> Field,
) : DexKitTarget<Field>(key, revision) {
    override fun find(bridge: DexKitBridge) = bridge.finder()
    override fun resolve(classLoader: ClassLoader, descriptor: String) = resolver(classLoader, descriptor)
}
