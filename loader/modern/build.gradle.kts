plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// The modern entrypoint receives callbacks for every enabled scope. Embed the normalized global
// allowlist here so an accidentally broad framework scope cannot initialize hooks in other apps.
val xposedTargetPackages = providers.gradleProperty("xposedTargetPackages").get()
    .split(',').map(String::trim).filter(String::isNotEmpty).distinct().joinToString(",")

android {
    namespace = "dev.xuanran.xposed.loader.modern"
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
    compileOnly(libs.libxposed.api)
}
