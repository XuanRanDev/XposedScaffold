package dev.xuanran.xposed.api

/**
 * 所有功能的公共契约。
 *
 * 实现通常继承 runtime 模块中的 SwitchHook、ActionHook 或 ApiHook，而不是直接实现此接口。
 */
interface HookFeature {
    /** 来自 [HookItem] 的展示与运行约束。 */
    val metadata: HookMetadata
    /** 轻量目标声明；实际 DexKit 查询由 core:dexkit 的 DexKitFeature 提供。 */
    val dexTargets: List<DexTargetSpec> get() = emptyList()
    /** 由设置页自动渲染的标准配置项。 */
    val options: List<HookOption> get() = emptyList()

    /** 在安装 Hook 前执行的额外环境判断。 */
    fun isAvailable(environment: HostEnvironment): Availability = Availability.Available
    /** 安装功能所需的全部 Hook；异常会被 Runtime 隔离并记录。 */
    fun install(context: HookContext)
    /** 可选卸载逻辑；并非所有 ART Hook 框架都保证即时卸载。 */
    fun unload() = Unit
}

/** 不依赖 Compose 的声明式配置项，UI 可据此自动生成控件。 */
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

data class Choice(
    val value: String,
    val label: String,
)

/** A stable stored value with a user-facing label; labels may be changed without migrating config. */
data class ChoiceOption(
    override val key: String,
    override val title: String,
    override val description: String = "",
    val default: String,
    val choices: List<Choice>,
) : HookOption {
    init {
        require(choices.isNotEmpty()) { "ChoiceOption must contain at least one choice" }
        require(choices.map(Choice::value).distinct().size == choices.size) { "Choice values must be unique" }
        require(default in choices.map(Choice::value)) { "ChoiceOption default must match a choice value" }
    }
}

/** 功能针对当前宿主环境的可用性结果。 */
sealed interface Availability {
    data object Available : Availability
    data class Unavailable(val reason: String) : Availability
}

/** 用于日志和状态页展示的轻量 Dex 目标信息。 */
data class DexTargetSpec(
    val key: String,
    val revision: Int = 1,
)

/**
 * 功能初始化期间的依赖容器。
 * 将环境、Hook、DexKit、配置和错误通道集中传入，避免使用难以测试的全局变量。
 */
class HookContext(
    val hookId: String,
    val environment: HostEnvironment,
    val dex: DexResolver,
    val config: HookConfig,
    val errors: ErrorReporter,
) {
    val bridge: HookBridge get() = environment.hookBridge
}

/** Runtime 面向 DexKit 层的稳定接口。 */
interface DexResolver {
    fun resolve(feature: HookFeature): Boolean
    fun method(key: String): java.lang.reflect.Method
    fun field(key: String): java.lang.reflect.Field
    fun invalidate(featureId: String)
}

/** 模块设置与宿主进程共享的类型安全最小配置接口。 */
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
