# Hook 开发指南

本文说明如何在当前脚手架中新增、配置和调试 Hook。示例均使用项目现有 API，不直接依赖 Xposed API 82 或 libxposed API。

## 选择功能类型

| 基类 | 设置页表现 | 宿主启动时行为 | 典型用途 |
| --- | --- | --- | --- |
| `SwitchHook` | 开关 | 开启后安装 | 隐藏元素、修改参数或返回值 |
| `ApiHook` | “自动” | 始终安装 | 公共状态、基础适配能力 |
| `ActionHook` | “执行”按钮 | 不进入宿主 | 导出、诊断、清理等模块端操作 |

绝大多数业务功能应该使用 `SwitchHook`。只有其他 Hook 确实依赖某项常驻能力时才使用 `ApiHook`。

## 创建 SwitchHook

下面的功能在 Activity 创建后记录类名，并允许用户配置日志前缀：

```kotlin
package dev.example.module.hooks

import android.app.Activity
import android.os.Bundle
import android.util.Log
import dev.xuanran.xposed.api.HookContext
import dev.xuanran.xposed.api.HookItem
import dev.xuanran.xposed.api.ModuleConfig
import dev.xuanran.xposed.api.RestartPolicy
import dev.xuanran.xposed.api.StringOption
import dev.xuanran.xposed.api.hook
import dev.xuanran.xposed.runtime.SwitchHook
import dev.xuanran.xposed.runtime.optionKey

@HookItem(
    id = "lifecycle.activity_log",
    path = "Lifecycle/Diagnostics",
    title = "记录 Activity 创建",
    description = "在框架日志中记录新建的 Activity。",
    keywords = ["activity", "log"],
    targetPackages = ["com.example.target"],
    targetProcesses = ["main"],
    restartPolicy = RestartPolicy.HOST,
)
object ActivityLogHook : SwitchHook() {
    override val options = listOf(
        StringOption(
            key = "prefix",
            title = "日志前缀",
            default = "Activity created",
        ),
    )

    override fun install(context: HookContext) {
        val prefix = context.config.getString(
            optionKey(metadata.id, "prefix"),
            "Activity created",
        )
        val onCreate = Activity::class.java.getDeclaredMethod(
            "onCreate",
            Bundle::class.java,
        )

        context.hook(onCreate) {
            after {
                context.bridge.log(
                    Log.INFO,
                    ModuleConfig.LOG_TAG,
                    "$prefix: ${thisObject?.javaClass?.name}",
                )
            }
        }
    }
}
```

编译时，KSP 会验证它是 Kotlin `object`，并把它加入生成的注册表。Hook ID 重复会直接产生编译错误。

## 配置 HookItem

`@HookItem` 同时控制设置页元数据和运行时过滤：

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `id` | 必填 | 全局唯一且发布后不可修改，也是配置命名空间 |
| `path` | 必填 | 使用 `/` 分隔的设置页分类 |
| `title` | 必填 | 用户可见标题 |
| `description` | 空 | 功能说明 |
| `keywords` | 空 | 设置页搜索关键词 |
| `targetPackages` | 空 | 功能允许运行的包；仍受全局包白名单限制 |
| `targetProcesses` | `main` | 允许的进程 |
| `minHostVersion` | `0` | 最低宿主 versionCode |
| `maxHostVersion` | `Long.MAX_VALUE` | 最高宿主 versionCode |
| `restartPolicy` | `HOST` | 修改配置后需要的重启范围 |
| `experimental` | `false` | 标记实验功能 |

进程规则：

- `main`：宿主主进程，即进程名等于包名。
- `*` 或 `any`：任意进程。
- `com.example.target:push`：精确匹配指定进程。
- 空数组：任意进程。

全局 `xposedTargetPackages` 是 Loader 的第一层白名单；单个 Hook 的 `targetPackages` 只能继续收窄，不能绕过全局白名单。

## 使用 before 和 after

`context.hook()` 接受 `Method` 或 `Constructor`，并返回可取消的 `UnhookHandle`。

修改实参：

```kotlin
context.hook(method) {
    before {
        args[0] = "replacement"
    }
}
```

跳过原方法并返回固定结果：

```kotlin
context.hook(method) {
    before {
        result = false
    }
}
```

修改原方法的返回结果：

```kotlin
context.hook(method) {
    after {
        val original = result as? String ?: return@after
        result = original.trim()
    }
}
```

替换异常：

```kotlin
context.hook(method) {
    after {
        if (throwable != null) {
            throwable = null
            result = emptyList<Any>()
        }
    }
}
```

设置 `result` 或 `throwable` 的具体提前返回语义由 Bridge 对应的框架实现负责。回调异常会被捕获、写入功能错误记录并输出框架日志，不会中断其他功能。

优先级默认是 `50`。数值越大，`before` 越早、`after` 越晚：

```kotlin
context.hook(method, priority = 100) {
    before { /* high-priority callback */ }
}
```

## 添加标准配置项

设置页支持布尔值、文本、整数范围和稳定值单选等声明式配置：

```kotlin
override val options = listOf(
    BooleanOption(
        key = "logging",
        title = "记录日志",
        default = true,
    ),
    StringOption(
        key = "replacement",
        title = "替换文本",
        default = "Example",
    ),
    IntRangeOption(
        key = "limit",
        title = "数量限制",
        default = 10,
        range = 1..100,
    ),
    ChoiceOption(
        key = "mode",
        title = "运行模式",
        default = "safe",
        choices = listOf(
            Choice("safe", "兼容"),
            Choice("fast", "快速"),
        ),
    ),
)
```

读取配置时必须通过 `optionKey()` 生成完整键：

```kotlin
val logging = context.config.getBoolean(
    optionKey(metadata.id, "logging"),
    true,
)
```

最终键格式为 `hook.<hook-id>.option.<option-key>`。不要用标题作为 key；标题可以翻译或修改，`id` 和 `key` 必须保持稳定。

## 创建 ApiHook

`ApiHook` 不读取启用开关，只要环境过滤通过就自动安装：

```kotlin
@HookItem(
    id = "api.activity_tracker",
    path = "Infrastructure",
    title = "Activity 跟踪器",
    targetPackages = ["com.example.target"],
)
object ActivityTracker : ApiHook() {
    @Volatile
    var currentActivity: Activity? = null
        private set

    override fun install(context: HookContext) {
        val onResume = Activity::class.java.getDeclaredMethod("onResume")
        context.hook(onResume) {
            after { currentActivity = thisObject as? Activity }
        }
    }
}
```

`ApiHook` 仍然会执行包名、进程、宿主版本和 `isAvailable()` 检查。

## 创建 ActionHook

`ActionHook` 只在模块应用进程执行，不会在宿主中调用 `install()`：

```kotlin
@HookItem(
    id = "action.connection_test",
    path = "Tools/Diagnostics",
    title = "测试设置页",
    restartPolicy = RestartPolicy.NONE,
)
object ConnectionTestAction : ActionHook() {
    override fun run(context: Context) {
        Toast.makeText(context, "模块应用运行正常", Toast.LENGTH_SHORT).show()
    }
}
```

`run()` 收到的是模块应用的 `Context`，不是宿主 `HookContext`，因此不能从这里直接安装宿主 Hook。

## 限制可用环境

需要比注解更复杂的判断时覆盖 `isAvailable()`：

```kotlin
override fun isAvailable(environment: HostEnvironment): Availability {
    return if (environment.hookBridge.apiVersion >= 101) {
        Availability.Available
    } else {
        Availability.Unavailable("需要 Xposed API 101 或更高版本")
    }
}
```

返回 `Unavailable` 后，该功能状态会变为 Unsupported，原因会写入状态记录和框架日志。

## 理解生命周期

宿主启动时按以下顺序处理每个功能：

1. Loader 全局包名白名单。
2. Hook 的包名过滤。
3. 进程过滤。
4. 宿主 versionCode 范围过滤。
5. `isAvailable()`。
6. 跳过 `ActionHook`。
7. 检查 `SwitchHook` 开关；`ApiHook` 视为始终开启。
8. 恢复或计算 DexKit 目标。
9. 调用 `install()`。
10. 更新为 Active，或记录具体失败阶段。

设置页修改开关不会卸载当前进程中已经安装的 ART Hook。通常需要按照 `restartPolicy` 重启宿主。虽然 `HookFeature` 提供 `unload()`，当前启动协调器不会自动调用它。

## 排查常见问题

| 现象 | 优先检查 |
| --- | --- |
| 设置页没有功能 | 是否为 `object`、是否添加 `@HookItem`、KSP 是否成功运行 |
| 功能一直 Disabled | 设置开关、Remote Preferences/New XSharedPreferences 连接状态 |
| 功能没有进入目标进程 | 全局包白名单、`targetPackages`、`targetProcesses` |
| 功能显示 Unsupported | versionCode 范围、`isAvailable()` 原因、DexKit 目标 |
| 宿主启动崩溃 | 是否绕过 `context.hook()` 直接调用 Loader、反射签名是否准确 |
| 改开关没有立即变化 | 杀死并重新启动目标宿主进程 |
| Release 入口不加载 | 检查 `app/proguard-rules.pro` 和打包后的 Xposed 元数据 |

## 验证清单

- Hook 关闭时不安装。
- Hook 开启后主进程行为正确。
- 不相关包与附加进程不会初始化重型对象。
- 正常结果、空结果、异常结果都不会崩溃。
- 重复调用不会产生状态泄漏。
- Legacy 和 Modern 变体都能编译。
- 修改配置后重启策略与 UI 描述一致。
- Release R8 后反射入口仍然存在。
