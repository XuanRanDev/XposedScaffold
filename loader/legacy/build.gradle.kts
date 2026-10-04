plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

val xposedTargetPackage = providers.gradleProperty("xposedTargetPackage").get()

android {
    namespace = "dev.xuanran.xposed.loader.legacy"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        buildConfigField("String", "XPOSED_TARGET_PACKAGE", "\"$xposedTargetPackage\"")
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
