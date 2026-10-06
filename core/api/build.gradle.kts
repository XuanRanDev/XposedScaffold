plugins {
    id("xposed.android.library")
}

val xposedModuleName = providers.gradleProperty("xposedModuleName").get()
val xposedModulePackage = providers.gradleProperty("xposedModulePackage").get()
val xposedLogTag = providers.gradleProperty("xposedLogTag").get()
val xposedPreferencesName = providers.gradleProperty("xposedPreferencesName").get()

android {
    namespace = "dev.xuanran.xposed.api"
    defaultConfig {
        buildConfigField("String", "MODULE_NAME", "\"$xposedModuleName\"")
        buildConfigField("String", "MODULE_PACKAGE", "\"$xposedModulePackage\"")
        buildConfigField("String", "LOG_TAG", "\"$xposedLogTag\"")
        buildConfigField("String", "PREFERENCES_NAME", "\"$xposedPreferencesName\"")
    }
    buildFeatures { buildConfig = true }
}
