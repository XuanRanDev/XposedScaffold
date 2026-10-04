plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

val xposedModuleName = providers.gradleProperty("xposedModuleName").get()
val xposedModulePackage = providers.gradleProperty("xposedModulePackage").get()
val xposedLogTag = providers.gradleProperty("xposedLogTag").get()
val xposedPreferencesName = providers.gradleProperty("xposedPreferencesName").get()

android {
    namespace = "dev.xuanran.xposed.api"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        buildConfigField("String", "MODULE_NAME", "\"$xposedModuleName\"")
        buildConfigField("String", "MODULE_PACKAGE", "\"$xposedModulePackage\"")
        buildConfigField("String", "LOG_TAG", "\"$xposedLogTag\"")
        buildConfigField("String", "PREFERENCES_NAME", "\"$xposedPreferencesName\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }
