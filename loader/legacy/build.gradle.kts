plugins {
    id("xposed.android.library")
}

// Keep loader filtering independent from the app module: the loader is packaged as a library and
// therefore needs its own generated constant from the shared root property.
val xposedTargetPackages = providers.gradleProperty("xposedTargetPackages").get()
    .split(',').map(String::trim).filter(String::isNotEmpty).distinct().joinToString(",")

android {
    namespace = "dev.xuanran.xposed.loader.legacy"
    defaultConfig {
        buildConfigField("String", "XPOSED_TARGET_PACKAGES", "\"$xposedTargetPackages\"")
    }
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation(projects.core.api)
    implementation(projects.core.runtime)
    implementation(projects.loader.startup)
    compileOnly(libs.xposed.api)
}
