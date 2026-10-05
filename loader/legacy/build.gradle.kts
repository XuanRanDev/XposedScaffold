plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// Keep loader filtering independent from the app module: the loader is packaged as a library and
// therefore needs its own generated constant from the shared root property.
val xposedTargetPackages = providers.gradleProperty("xposedTargetPackages").get()
    .split(',').map(String::trim).filter(String::isNotEmpty).distinct().joinToString(",")

android {
    namespace = "dev.xuanran.xposed.loader.legacy"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        buildConfigField("String", "XPOSED_TARGET_PACKAGES", "\"$xposedTargetPackages\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(projects.core.api)
    implementation(projects.loader.startup)
    compileOnly(libs.xposed.api)
}
