# DexKit 使用指南

DexKit 用于在宿主代码被混淆、类名和方法名不稳定时，根据字符串、调用关系、参数、返回类型等结构特征定位目标成员。

本项目已经封装 DexKit 生命周期与持久化缓存。业务功能不应自行反复创建 `DexKitBridge`，而应实现 `DexKitFeature` 并声明 `MethodTarget` 或 `FieldTarget`。

## 何时使用 DexKit

适合使用 DexKit：

- 类名或方法名每个版本都会混淆变化。
- 方法内部存在相对稳定的字符串、数字或调用关系。
- 可以组合多个条件把结果收敛为唯一成员。

不需要使用 DexKit：

- Android SDK 方法，例如 `Activity.onCreate()`。
- 名称和签名长期稳定的公开宿主 API。
- 已经能通过稳定接口、父类或资源 ID 定位的目标。

DexKit 搜索比普通反射昂贵。优先使用稳定反射，只有混淆目标才进入 DexKit。

## 完整方法示例

下面假设目标方法位于 `com.example.target`，内部同时使用字符串 `User profile loaded` 和 `profile_id`。方法名和类名可以被混淆，但这两个业务字符串在目标版本中保持稳定。

```kotlin
package dev.example.module.hooks

import android.util.Log
import dev.xuanran.xposed.api.HookContext
import dev.xuanran.xposed.api.HookItem
import dev.xuanran.xposed.api.ModuleConfig
import dev.xuanran.xposed.api.hook
import dev.xuanran.xposed.dexkit.DexKitFeature
import dev.xuanran.xposed.dexkit.MethodTarget
import dev.xuanran.xposed.runtime.SwitchHook
import org.luckypray.dexkit.wrap.DexMethod

@HookItem(
    id = "profile.trace_load",
    path = "Profile/Diagnostics",
    title = "跟踪资料加载",
    targetPackages = ["com.example.target"],
)
object TraceProfileLoadHook : SwitchHook(), DexKitFeature {
    private const val LOAD_PROFILE_METHOD =
        "profile.trace_load.load_profile_method"

    override val dexKitTargets = listOf(
        MethodTarget(
            key = LOAD_PROFILE_METHOD,
            revision = 1,
            finder = {
                findMethod {
                    searchPackages("com.example.target")
                    matcher {
                        usingStrings("User profile loaded", "profile_id")
                        returnType = "void"
                        paramCount = 1
                    }
                }.single().descriptor
            },
            resolver = { classLoader, descriptor ->
                DexMethod(descriptor)
                    .getMethodInstance(classLoader)
                    .apply { isAccessible = true }
            },
        ),
    )

    override fun install(context: HookContext) {
        val method = context.dex.method(LOAD_PROFILE_METHOD)
        context.hook(method) {
            after {
                context.bridge.log(
                    Log.INFO,
                    ModuleConfig.LOG_TAG,
                    "Profile loader called: $executable",
                )
            }
        }
    }
}
```

进入 `install()` 前，Runtime 已经解析完该功能声明的全部目标：

```text
HookRuntime
    -> DexKitResolver.resolve(feature)
    -> 尝试读取并恢复缓存
    -> 缓存不存在或恢复失败时创建 DexKitBridge
    -> 执行 MethodTarget.find()
    -> 保存描述符并缓存 Method
    -> HookFeature.install()
    -> context.dex.method(key)
```

## 声明 MethodTarget

| 参数 | 说明 |
| --- | --- |
| `key` | 获取解析结果时使用的稳定键；当前实现要求所有功能之间也保持唯一 |
| `revision` | 查询规则版本；修改匹配条件时递增 |
| `finder` | 只在缓存失效时执行，返回可持久化描述符 |
| `resolver` | 每次进程启动把描述符恢复为当前 ClassLoader 下的 `Method` |

推荐 key 格式为 `<hook-id>.<member-purpose>`，例如：

```text
profile.trace_load.load_profile_method
```

持久化缓存包含 Hook ID，但当前进程内解析表通过 `target.key` 取值，所以不要在两个功能中重复使用 `target_method` 这类简单 key。

## 声明 FieldTarget

字段目标与方法目标流程相同。以下代码需要导入 `org.luckypray.dexkit.wrap.DexField`：

```kotlin
private const val USER_ID_FIELD = "profile.user_state.user_id_field"

override val dexKitTargets = listOf(
    FieldTarget(
        key = USER_ID_FIELD,
        revision = 1,
        finder = {
            findField {
                searchPackages("com.example.target")
                matcher {
                    type = "java.lang.String"
                }
            }.single().descriptor
        },
        resolver = { classLoader, descriptor ->
            DexField(descriptor)
                .getFieldInstance(classLoader)
                .apply { isAccessible = true }
        },
    ),
)

override fun install(context: HookContext) {
    val field = context.dex.field(USER_ID_FIELD)
    // 在已经确认类型的目标对象实例上调用 field.get(instance)。
}
```

字段查询只写 `type` 通常不足以保证唯一。实际功能应增加声明类特征、使用关系或其他稳定条件，并用 `.single()` 验证唯一性。

## 编写可靠查询

优先组合多个互相独立的条件：

```kotlin
findMethod {
    searchPackages("com.example.target.feature")
    excludePackages("com.example.target.thirdparty")
    matcher {
        usingStrings("User profile loaded", "profile_id")
        returnType = "void"
        paramCount = 1
    }
}.single()
```

建议：

- 使用完整业务字符串，而不是 `ok`、`id` 之类短字符串。
- 限定 `searchPackages`，减少扫描范围和误匹配。
- 组合字符串、返回类型、参数数量、调用关系等特征。
- 使用 `.single()`，让零结果和多结果都明确失败，不要悄悄选错目标。
- 不依赖单个混淆类名或方法名。
- 不把某个版本的反编译行号、DEX 偏移作为跨版本特征。
- `findMethod` 中避免嵌套代价高的复杂 `declaredClass` 条件；必要时先查类，再在结果内查方法。

查询返回的 `MethodData.descriptor` 是 Dalvik 描述符，例如：

```text
Lcom/example/a;->b(Ljava/lang/String;)Z
```

它比 Java 反射对象适合持久化，因为反射对象只属于当前进程和 ClassLoader。

## 理解缓存

项目把描述符保存到独立 SharedPreferences：

```text
<xposedPreferencesName>_dex
```

缓存键格式：

```text
dex.<hook-id>.<target-key>.v<host-version-code>.r<revision>
```

示例：

```text
dex.profile.trace_load.profile.trace_load.load_profile_method.v12045.r1
```

首次运行：

1. 没有对应缓存。
2. 创建一次 `DexKitBridge`。
3. 执行查询并获得描述符。
4. 使用当前宿主 ClassLoader 恢复 `Method`/`Field`。
5. 恢复成功后保存描述符。

后续运行：

1. 读取描述符。
2. 使用当前 ClassLoader 再次恢复反射成员。
3. 恢复成功则完全跳过 DexKit 扫描。
4. 恢复失败则把该目标视为未解析并重新扫描。

缓存不会保存 `Method`、`Field` 或 DexKit native 对象，只保存可移植描述符。

## 处理宿主版本变化

`hostVersion` 使用宿主 APK 的 `versionCode`，并包含在缓存键中：

```text
v12045.r1 -> v12046.r1
```

只要宿主正常递增 versionCode，新版本首次启动就会生成新缓存，不会复用旧版本描述符。

如果宿主厂商不改变 versionCode 就替换 APK，会出现一个边界情况：

- 旧描述符无法恢复：自动重新扫描。
- 旧描述符仍能恢复但语义改变：当前实现无法发现，仍会使用旧成员。

对这种发布方式有强需求时，可以扩展缓存键，加入 APK 哈希、签名摘要或 `lastUpdateTime`。修改缓存键属于核心行为变更，必须增加测试。

## 修改查询规则

只修改查询代码不会改变宿主 versionCode。每次改变匹配语义时都要递增 `revision`：

```kotlin
MethodTarget(
    key = LOAD_PROFILE_METHOD,
    revision = 2,
    finder = {
        // 此处使用新查询条件并返回唯一结果的 descriptor。
    },
    resolver = { classLoader, descriptor ->
        DexMethod(descriptor).getMethodInstance(classLoader)
    },
)
```

下列修改需要递增 revision：

- 增删字符串条件。
- 改变参数、返回类型或修饰符条件。
- 改变查询包范围。
- 改变结果选择逻辑。
- resolver 对描述符的解释发生变化。

只修改 Hook 回调逻辑而查询目标不变时，不需要递增。

## 主动清除功能缓存

`DexResolver` 支持按功能 ID 清除缓存：

```kotlin
context.dex.invalidate(metadata.id)
```

它会删除该功能所有 `dex.<feature-id>.*` 持久化条目，不影响其他功能。当前进程已经解析的成员仍保存在内存中，因此清除后应重启宿主进程，下一次初始化才会重新扫描。

旧版本号和旧 revision 产生的条目不会自动清理，但不会再命中新缓存键。需要长期控制配置文件大小时，可以在模块升级迁移逻辑中清理历史前缀。

## 失败与状态

| 失败位置 | 结果 |
| --- | --- |
| 查询零结果或多结果 | `single()` 抛出异常，功能进入 DexResolution Failed |
| 缓存描述符无法恢复 | 自动回退到 DexKit 查询 |
| 新查询仍无法恢复成员 | 功能进入 DexResolution Failed |
| `context.dex.method(key)` 使用错误 key | 安装阶段失败 |
| 一个功能解析失败 | 只影响该功能，不阻止其他 Hook |

查询异常会被 Runtime 记录并写入框架日志。不要捕获查询异常后返回任意候选，否则会把明确的适配失败变成难以定位的错误 Hook。

## 性能与线程注意事项

- DexKit 在宿主 `Application.attach` 之后、功能安装之前同步解析。
- 只有缓存缺失或描述符恢复失败时才创建 `DexKitBridge`。
- 同一个功能的多个未解析目标共用一次 Bridge 生命周期。
- Bridge 使用 `use` 自动关闭 native 资源。
- 大量或复杂查询会增加宿主冷启动时间。
- 生产模块应缩小包范围和查询集合，不要为普通稳定方法使用 DexKit。

当前脚手架没有单独的“预适配宿主”后台任务；启用功能后的首次宿主启动就是实际扫描时机。

## 验证清单

- 干净缓存下能找到唯一结果。
- 第二次启动从缓存恢复，不重新扫描。
- 缓存描述符损坏时能够重新扫描。
- 宿主 versionCode 改变后使用新缓存键。
- 查询规则改变后已递增 revision。
- 方法参数、返回类型和静态/实例属性符合预期。
- Hook 在目标的冷启动、热启动和重复调用下稳定。
- 不同功能的 target key 全局唯一。
- Legacy 与 Modern 变体均能编译并运行。

项目中的可编译示例见 [`DexKitSampleHook.kt`](../app/src/main/kotlin/dev/xuanran/xposedscaffold/sample/DexKitSampleHook.kt)。
