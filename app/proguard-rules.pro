# Xposed frameworks instantiate these entry points from packaged metadata rather than code references.
-keep class dev.xuanran.xposed.loader.legacy.LegacyXposedEntry { public <init>(); }
-keep class dev.xuanran.xposed.loader.modern.ModernXposedEntry { public <init>(); }

# BaseHookFeature reads @HookItem and its default values at runtime. R8 may otherwise keep the
# generated object reference while discarding the class annotation, causing the settings Activity
# to fail as soon as it evaluates feature.metadata.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# R8 full mode treats annotations and annotated classes as separate liveness endpoints. Keeping
# only the attribute is not enough: pin the annotation type and every HookItem endpoint while
# still allowing their implementation names and bytecode to be optimized.
-keep @interface dev.xuanran.xposed.api.HookItem
-keep,allowoptimization,allowobfuscation @dev.xuanran.xposed.api.HookItem class *

# ModuleStartup loads the KSP-generated top-level createHooks function by its stable JVM name.
-keep class dev.xuanran.xposed.generated.GeneratedHookRegistryKt {
    public static java.util.List createHooks();
}

# These APIs are compileOnly by design and are supplied by the active Xposed framework at runtime.
-dontwarn de.robv.android.xposed.**
-dontwarn io.github.libxposed.api.**
