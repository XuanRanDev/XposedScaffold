# Xposed Scaffold

## 模板配置

创建新模块时，集中修改根目录 `gradle.properties` 中的以下字段即可：

```properties
xposedTargetPackage=com.example.target
xposedProjectName=XposedScaffold
xposedModuleName=Xposed Scaffold
xposedModulePackage=dev.xuanran.xposedscaffold
xposedLogTag=XposedScaffold
xposedPreferencesName=xposed_scaffold
```

其中 `xposedModulePackage` 控制最终 APK 的 applicationId。源码 namespace 和 Kotlin 包路径是脚手架内部实现，通常无需随模块身份修改。

A modern, reusable Xposed module foundation under the `dev.xuanran` namespace.

The project borrows the proven separation used by mature modules such as QAuxiliary—framework loaders, startup, hook API, runtime and feature code are separate—but intentionally leaves out native hooks, hidden DEX, SO protection, Frida and hot updates.

## What is included

- A `legacy` flavor containing only the Xposed API 82 entry.
- A `modern` flavor containing only the libxposed API 101–102 entry
  (`io.github.libxposed:api:102.0.0`).
- A framework-neutral `HookBridge` and small hook DSL.
- `@HookItem` plus KSP-generated, type-safe feature registry.
- Automatic Material 3 UI for switch, action and always-on API features.
- Package, process and host-version filtering.
- Per-feature lifecycle state and isolated error records.
- DexKit as a normal Maven dependency, with versioned descriptor caching.
- A replaceable configuration interface and remote-preference support.
- A harmless sample hook and a placeholder target package.

## Modules

```text
app                 Compose settings UI and project features
core:api            Stable contracts used across all layers
core:runtime        Feature registry, lifecycle, state and config
core:dexkit         Optional DexKit target and cache support
loader:startup      Shared Application.attach startup path
loader:legacy       Xposed API 82 adapter
loader:modern       libxposed API 101–102 adapter
processor           KSP feature registry generator
```

No feature code should depend directly on a loader. Ordinary features depend on `core:runtime`; DexKit features additionally depend on `core:dexkit`.

## Start a real module

1. Replace `dev.xuanran.xposedscaffold` with your final application ID.
2. Set `xposedTargetPackage` once in the root `gradle.properties`. Gradle injects it into
   the sample metadata, legacy loader, modern loader and legacy recommended scope.
3. Rename the application and description.
4. Delete or rewrite `SampleHook`.
5. Keep feature IDs stable after publishing; they are configuration keys.

## Add a feature

```kotlin
@HookItem(
    id = "chat.hide_banner",
    path = "Chat/Appearance",
    title = "Hide chat banner",
    description = "Removes the banner above the conversation.",
    targetPackages = ["com.example.target"],
    targetProcesses = ["main"],
)
object HideChatBanner : SwitchHook() {
    override fun install(context: HookContext) {
        val method = context.environment.classLoader
            .loadClass("com.example.target.ChatActivity")
            .getDeclaredMethod("showBanner")

        context.hook(method) {
            before { result = null }
        }
    }
}
```

KSP adds the object to `GeneratedHookRegistry`. The settings app automatically displays its title, description, path, switch, restart policy, packages and processes.

## DexKit features

Implement `DexKitFeature` and expose one or more `MethodTarget`/`FieldTarget` objects. Each target has:

- a stable key;
- a revision that you increment when the matching rule changes;
- a DexKit search that returns a portable descriptor string;
- a resolver that converts the descriptor into a reflection member for the current host class loader.

The runtime caches by feature ID, target key, host version and target revision. Failed or stale resolutions prevent only that feature from loading.

Keep expensive searches out of ordinary startup where possible. A production project should expose a user-triggered “adapt host” screen and persist results before enabling affected features.

## Configuration note

Modern API 102 uses framework-provided Remote Preferences. The module settings app obtains
`XposedService` and writes to LSPosed's preference database; the hooked process reads the same
group through `XposedModule.getRemotePreferences()`.
Open the module settings page only after enabling the module in a compatible LSPosed build. The
status card must show `Remote Preferences 已连接`; restart the target application after changing a
switch so the hook runtime is initialized from the new value.

The legacy flavor keeps the API 82 entrypoint and uses LSPosed's New XSharedPreferences extension.
It declares `xposedsharedprefs`, opens the module-side file with `MODE_WORLD_READABLE`, and reads it
from the hooked process with `XSharedPreferences(modulePackage, preferenceName)`. This requires an
LSPosed version that implements New XSharedPreferences; plain/older API 82 frameworks are not
supported for remote switches. On an unsupported framework the settings page disables switches;
use `ApiHook` or provide your own in-host configuration implementation instead.

## Design rules

- Entry classes contain no feature logic.
- Loader code must stay Java/Kotlin-runtime-light and must not initialize Compose.
- Feature failures are isolated and recorded; never crash all features for one failed hook.
- Avoid runtime DEX scanning for registration; KSP owns the registry.
- Host classes belong in a separate `compileOnly` stub module when needed.
- Do not put target-specific constants in `core` or `loader`.
- Prefer descriptors and verified reflection members over hard-coded obfuscated names.
- Keep protection, telemetry and network services out of the base scaffold.

## Build debug APKs

Open the root directory in Android Studio with JDK 17 and Android SDK 36 installed.
Choose the flavor that matches the installed framework:

```powershell
# Compatible with traditional LSPosed/Xposed API 82 loaders.
.\gradlew.bat :app:assembleLegacyDebug

# Requires a framework that genuinely implements libxposed API 101 or 102.
.\gradlew.bat :app:assembleModernDebug
```

Both variants deliberately use the same application ID, so they replace each
other rather than appearing as two independent modules. Never publish a custom
variant that packages both loader modules: an API 100-era framework may see the
modern entry metadata but still reflect the obsolete two-argument constructor,
which fails before module initialization.

## 打包无签名 Release APK

### 选择正确的 flavor

| Flavor | 使用场景 | Release 任务 |
| --- | --- | --- |
| `legacy` | 传统 LSPosed、Xposed API 82，或出现 modern 入口双参数构造器错误的框架 | `:app:assembleLegacyRelease` |
| `modern` | 明确支持 libxposed API 101/102 的框架 | `:app:assembleModernRelease` |

两个 flavor 使用相同的 application ID，不能同时安装。项目位于
`E:\Code\Kotlin\XposedScaffold`，在 PowerShell 中直接执行以下一行命令即可。

构建传统 API 82 的无签名 Release：

```powershell
Set-Location -LiteralPath "E:\Code\Kotlin\XposedScaffold"; .\gradlew.bat :app:assembleLegacyRelease
```

构建 libxposed API 101/102 的无签名 Release：

```powershell
Set-Location -LiteralPath "E:\Code\Kotlin\XposedScaffold"; .\gradlew.bat :app:assembleModernRelease
```

一次构建两个无签名 Release：

```powershell
Set-Location -LiteralPath "E:\Code\Kotlin\XposedScaffold"; .\gradlew.bat :app:assembleRelease
```

默认输出位置：

```text
app/build/outputs/apk/legacy/release/app-legacy-release-unsigned.apk
app/build/outputs/apk/modern/release/app-modern-release-unsigned.apk
```

这些文件没有 Release 签名，不能直接覆盖安装到 Android 设备；本节仅用于生成
未签名的发布构建。首次使用或不确定框架 API 版本时，优先构建 `legacyRelease`。
