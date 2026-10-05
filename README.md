# Xposed Scaffold

一个同时支持传统 Xposed API 82 与 libxposed API 101–102 的 Kotlin 模块脚手架。

项目把框架入口、Hook 业务、DexKit、配置、设置界面和代码生成拆成独立模块。业务 Hook 只依赖稳定的 `HookContext` 与 `HookBridge`，同一份功能代码可以运行在 legacy 和 modern 两套 Loader 上。

> 重要：`legacy` 与 `modern` 使用相同 applicationId，不能同时安装。构建时必须选择与设备框架匹配的变体。

## 快速开始

### 1. 配置模块身份

编辑根目录的 `gradle.properties`：

```properties
xposedTargetPackages=com.example.target
xposedProjectName=ExampleModule
xposedModuleName=Example Module
xposedModulePackage=dev.example.module
xposedLogTag=ExampleModule
xposedPreferencesName=example_module
```

| 属性 | 用途 |
| --- | --- |
| `xposedTargetPackages` | 模块允许注入的宿主包名，多个包使用逗号分隔 |
| `xposedProjectName` | Gradle 根项目名称 |
| `xposedModuleName` | 设置页显示名称 |
| `xposedModulePackage` | 最终 APK applicationId |
| `xposedLogTag` | 框架日志标签 |
| `xposedPreferencesName` | 模块与宿主共享的配置文件名 |

源码 namespace 和 Kotlin 包路径属于脚手架内部结构，创建新模块时通常不需要整体重命名。

### 2. 编写第一个 Hook

在 `app/src/main/kotlin` 下创建 Kotlin `object`，添加 `@HookItem`：

```kotlin
import android.app.Activity
import android.os.Bundle
import android.util.Log
import dev.xuanran.xposed.api.HookContext
import dev.xuanran.xposed.api.HookItem
import dev.xuanran.xposed.api.ModuleConfig
import dev.xuanran.xposed.api.hook
import dev.xuanran.xposed.runtime.SwitchHook

@HookItem(
    id = "example.activity_log",
    path = "Example/Lifecycle",
    title = "记录 Activity 创建",
    targetPackages = ["com.example.target"],
)
object ActivityLogHook : SwitchHook() {
    override fun install(context: HookContext) {
        val method = Activity::class.java.getDeclaredMethod(
            "onCreate",
            Bundle::class.java,
        )
        context.hook(method) {
            after {
                context.bridge.log(
                    Log.INFO,
                    ModuleConfig.LOG_TAG,
                    "Created: ${thisObject?.javaClass?.name}",
                )
            }
        }
    }
}
```

不需要维护注册表。KSP 会在编译期收集全部 `@HookItem`，生成 `GeneratedHookRegistry`，设置页也会自动出现对应条目。

完整说明见 [Hook 开发指南](docs/hook-development.md)。

### 3. 构建 APK

项目需要 JDK 17 和 Android SDK。Windows PowerShell 命令：

```powershell
# 传统 Xposed / LSPosed API 82
.\gradlew.bat :app:assembleLegacyDebug

# 真正实现 libxposed API 101/102 的框架
.\gradlew.bat :app:assembleModernDebug
```

构建经过 R8 和资源压缩的无签名 Release：

```powershell
.\gradlew.bat :app:assembleLegacyRelease :app:assembleModernRelease
```

输出位置：

```text
app/build/outputs/apk/legacy/debug/app-legacy-debug.apk
app/build/outputs/apk/modern/debug/app-modern-debug.apk
app/build/outputs/apk/legacy/release/app-legacy-release-unsigned.apk
app/build/outputs/apk/modern/release/app-modern-release-unsigned.apk
```

Release APK 默认未签名，发布或覆盖安装前需要使用自己的签名配置。

## 选择 Loader

| 变体 | 框架入口 | 适用环境 | 配置通道 |
| --- | --- | --- | --- |
| `legacy` | Xposed API 82 `IXposedHookLoadPackage` | 传统 Xposed、兼容 API 82 的 LSPosed | New XSharedPreferences |
| `modern` | libxposed `XposedModule` | 明确支持 API 101/102 的框架 | Remote Preferences |

一个 APK 只包含一种入口 ABI。不要把 legacy 与 modern Loader 同时打进同一个自定义变体，否则框架可能选择错误入口，并在业务代码运行前失败。

Legacy 设置开关依赖框架实现 New XSharedPreferences；不支持该扩展的老框架无法可靠共享模块配置。Modern 设置页需要先在兼容框架中启用模块，等待状态页显示 Remote Preferences 已连接。

## 功能概览

- `SwitchHook`：由用户开关控制的宿主功能。
- `ApiHook`：无用户开关、在匹配宿主中自动安装的基础能力。
- `ActionHook`：只在模块设置应用内点击执行，不注入宿主。
- `before` / `after` Hook DSL：修改参数、结果或异常。
- `BooleanOption`、`StringOption`、`IntRangeOption`：自动生成标准设置控件。
- 包名、进程名、宿主版本和额外可用性过滤。
- 每项功能独立的运行状态与错误隔离。
- DexKit 混淆目标查找、描述符缓存和版本失效。
- KSP 自动注册，不做运行时 DEX 功能扫描。
- Compose Material 3 设置页。
- Release R8、资源压缩及必要的 Xposed 入口保留规则。

## 项目结构

```text
app                 模块应用、Compose 设置页和目标相关 Hook
core/api            HookFeature、HookContext、HookBridge 等稳定契约
core/runtime        功能生命周期、注册表、配置和状态管理
core/dexkit         DexKit 目标声明、描述符缓存和反射恢复
loader/startup      两套 Loader 共用的 Application.attach 启动流程
loader/legacy       Xposed API 82 适配器
loader/modern       libxposed API 101–102 适配器
processor           @HookItem KSP 注册表生成器
docs                开发指南
```

运行链路：

```text
Xposed Loader
    -> ModuleStartup
    -> 包名/进程/版本过滤
    -> 读取功能开关
    -> 恢复或计算 DexKit 目标
    -> HookFeature.install(HookContext)
    -> HookBridge 适配具体框架
```

业务代码不得直接依赖 `loader:legacy` 或 `loader:modern`。普通功能使用 `core:api`/`core:runtime`；需要识别混淆成员时，再使用 `core:dexkit`。

## 文档

- [Hook 开发指南](docs/hook-development.md)：功能类型、注解、生命周期、配置、回调和排错。
- [DexKit 使用指南](docs/dexkit.md)：查询、描述符、缓存、版本失效、性能和完整示例。
- [普通 Hook 示例](app/src/main/kotlin/dev/xuanran/xposedscaffold/sample/SampleHook.kt)
- [DexKit Hook 示例](app/src/main/kotlin/dev/xuanran/xposedscaffold/sample/DexKitSampleHook.kt)

## 开发约束

- 所有用户可见功能必须使用 `@HookItem`，不要创建手工注册表。
- `@HookItem` 只能标注实现 `HookFeature` 的 Kotlin `object`。
- 已发布的 Hook `id` 和配置项 `key` 必须保持稳定。
- 业务 Hook 使用 `HookContext`/`HookBridge`，不能调用具体 Loader API。
- 目标相关代码放在 `app`，不要污染脚手架核心模块。
- 混淆目标优先使用 DexKit，缓存可移植描述符，并在读取时恢复为反射成员。
- 修改 DexKit 查询规则时递增目标 `revision`。
- 修改处理器、生命周期过滤、Bridge 适配器或缓存键时必须增加测试。
- 不向核心引入 native Hook、隐藏 DEX、SO 保护、Frida、热更新或遥测。

## 验证改动

```powershell
# 核心单元测试
.\gradlew.bat :core:runtime:test :processor:test

# 验证两套入口和 Release 收缩规则
.\gradlew.bat :app:assembleLegacyRelease :app:assembleModernRelease
```

修改 Hook 后至少验证目标包、主进程、目标附加进程、开关关闭、开关开启和宿主升级后的首次启动。
