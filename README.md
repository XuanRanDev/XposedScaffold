# Xposed Scaffold

A modern, reusable Xposed module foundation under the `dev.xuanran` namespace.

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
2. Replace `com.example.target` in:
   - `app/src/main/res/values/strings.xml`
   - `loader/modern/src/main/resources/META-INF/xposed/scope.list`
   - `loader/legacy/.../TargetScope`
   - the sample `@HookItem` declaration
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

Modern libxposed uses framework-provided remote preferences. The legacy adapter uses `XSharedPreferences`. If a target framework cannot read the module preference XML, replace `HookConfig` with a ContentProvider or another cross-process implementation. The hook and UI layers do not need to change.

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

## 打包 Release APK

### 1. 选择正确的 flavor

| Flavor | 使用场景 | Release 任务 |
| --- | --- | --- |
| `legacy` | 传统 LSPosed、Xposed API 82，或出现 modern 入口双参数构造器错误的框架 | `:app:assembleLegacyRelease` |
| `modern` | 明确支持 libxposed API 101/102 的框架 | `:app:assembleModernRelease` |

两个 flavor 使用相同的 application ID，不能同时安装。升级已安装版本时还必须使用
相同的签名证书，否则 Android 会提示签名不一致。

### 2. 构建 unsigned Release

在项目根目录执行：

```powershell
# 传统 API 82 版本
.\gradlew.bat :app:assembleLegacyRelease

# libxposed API 101/102 版本
.\gradlew.bat :app:assembleModernRelease
```

也可以一次构建两个 Release：

```powershell
.\gradlew.bat :app:assembleRelease
```

默认输出位置：

```text
app/build/outputs/apk/legacy/release/app-legacy-release-unsigned.apk
app/build/outputs/apk/modern/release/app-modern-release-unsigned.apk
```

项目没有把发布证书写入仓库，因此命令行生成的是 unsigned APK，不能直接安装。
不要把 keystore、alias 密码或 store 密码提交到 Git。

### 3. 创建发布证书

只需创建一次，并妥善备份。丢失证书后将无法覆盖升级此前发布的 APK：

```powershell
keytool -genkeypair -v `
  -keystore release.jks `
  -alias release `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000
```

### 4. 对 APK 对齐并签名

`zipalign` 和 `apksigner` 位于 Android SDK 的
`build-tools/<版本>/` 目录。下面以 legacy 版本为例，请把
`<ANDROID_SDK>` 和 `<BUILD_TOOLS_VERSION>` 替换为本机实际路径：

```powershell
& "<ANDROID_SDK>\build-tools\<BUILD_TOOLS_VERSION>\zipalign.exe" `
  -p -f 4 `
  "app\build\outputs\apk\legacy\release\app-legacy-release-unsigned.apk" `
  "app\build\outputs\apk\legacy\release\app-legacy-release-aligned.apk"

& "<ANDROID_SDK>\build-tools\<BUILD_TOOLS_VERSION>\apksigner.bat" sign `
  --ks "release.jks" `
  --ks-key-alias "release" `
  --out "app\build\outputs\apk\legacy\release\app-legacy-release.apk" `
  "app\build\outputs\apk\legacy\release\app-legacy-release-aligned.apk"
```

modern 版本的步骤相同，只需把路径和文件名中的 `legacy` 替换为 `modern`。
`apksigner` 会交互式读取密码，避免把密码留在命令历史或脚本中。

### 5. 验证签名

```powershell
& "<ANDROID_SDK>\build-tools\<BUILD_TOOLS_VERSION>\apksigner.bat" verify `
  --verbose --print-certs `
  "app\build\outputs\apk\legacy\release\app-legacy-release.apk"
```

看到 `Verifies` 即表示签名验证通过。安装后还需要在 Xposed 管理器中启用模块、
选择目标作用域，并强制停止后重新启动目标应用。

### 使用 Android Studio 签名

不想手动调用签名工具时，可以选择：

```text
Build → Generate Signed App Bundle or APK → APK
```

选择 `app` 模块、发布证书、`release` Build Type，并确认构建的是
`legacyRelease` 或 `modernRelease`。首次使用或不确定框架 API 版本时，
优先构建 `legacyRelease`。
