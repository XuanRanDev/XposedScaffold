# Xposed frameworks instantiate these entry points and invoke their callbacks from packaged
# metadata. Keep every member: compileOnly framework APIs are absent during R8 analysis, so R8
# cannot otherwise prove that callback implementations such as handleLoadPackage are reachable.
-keep class dev.xuanran.xposed.loader.legacy.LegacyXposedEntry { *; }
-keep class dev.xuanran.xposed.loader.modern.ModernXposedEntry { *; }

# Hook callbacks cross the module/framework boundary too. Match the legacy ABI methods directly
# because compileOnly framework types are unavailable to R8's hierarchy analysis. The modern
# adapter uses a stable named Hooker so it can be kept without relying on synthetic lambda names.
-keepclasseswithmembers class * {
    protected void beforeHookedMethod(de.robv.android.xposed.XC_MethodHook$MethodHookParam);
    protected void afterHookedMethod(de.robv.android.xposed.XC_MethodHook$MethodHookParam);
}
-keep class dev.xuanran.xposed.loader.modern.ModernHooker { *; }

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
