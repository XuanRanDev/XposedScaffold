package dev.xuanran.xposed.api

interface HookFeature {
    val metadata: HookMetadata
    val dexTargets: List<DexTargetSpec> get() = emptyList()
    val options: List<HookOption> get() = emptyList()

    fun isAvailable(environment: HostEnvironment): Availability = Availability.Available
    fun install(context: HookContext)
    fun unload() = Unit
}

sealed interface HookOption {
    val key: String
    val title: String
    val description: String
}

data class BooleanOption(
    override val key: String,
    override val title: String,
    override val description: String = "",
    val default: Boolean = false,
) : HookOption

data class StringOption(
    override val key: String,
    override val title: String,
    override val description: String = "",
    val default: String = "",
) : HookOption

data class IntRangeOption(
    override val key: String,
    override val title: String,
    override val description: String = "",
    val default: Int,
    val range: IntRange,
) : HookOption

sealed interface Availability {
    data object Available : Availability
    data class Unavailable(val reason: String) : Availability
}

data class DexTargetSpec(
    val key: String,
    val revision: Int = 1,
)

class HookContext(
    val environment: HostEnvironment,
    val dex: DexResolver,
    val config: HookConfig,
    val errors: ErrorReporter,
) {
    val bridge: HookBridge get() = environment.hookBridge
}

interface DexResolver {
    fun resolve(feature: HookFeature): Boolean
    fun method(key: String): java.lang.reflect.Method
    fun field(key: String): java.lang.reflect.Field
    fun invalidate(featureId: String)
}

interface HookConfig {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
}

fun interface ErrorReporter {
    fun report(failure: HookFailure)
}
