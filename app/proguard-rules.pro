# Xposed frameworks instantiate these entry points from packaged metadata rather than code references.
-keep class dev.xuanran.xposed.loader.legacy.LegacyXposedEntry { public <init>(); }
-keep class dev.xuanran.xposed.loader.modern.ModernXposedEntry { public <init>(); }

# ModuleStartup loads the KSP-generated top-level createHooks function by its stable JVM name.
-keep class dev.xuanran.xposed.generated.GeneratedHookRegistryKt {
    public static java.util.List createHooks();
}

# These APIs are compileOnly by design and are supplied by the active Xposed framework at runtime.
-dontwarn de.robv.android.xposed.**
-dontwarn io.github.libxposed.api.**
