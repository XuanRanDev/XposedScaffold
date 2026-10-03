# Xposed Scaffold

A modern, reusable Xposed module foundation under the `dev.xuanran` namespace.

## What is included

- Legacy Xposed API 82 entry.
- Modern libxposed API 101–102 entry (`io.github.libxposed:api:102.0.0`).
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

## Build

Open the root directory in Android Studio with JDK 17 and Android SDK 36 installed, then run:

```shell
./gradlew :app:assembleDebug
```

The repository is intentionally unsigned beyond the normal Android debug signing configuration.
