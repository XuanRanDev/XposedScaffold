plugins {
    id("xposed.android.library")
}

// The modern entrypoint receives callbacks for every enabled scope. Embed the normalized global
// allowlist here so an accidentally broad framework scope cannot initialize hooks in other apps.
val xposedTargetPackages = providers.gradleProperty("xposedTargetPackages").get()
    .split(',').map(String::trim).filter(String::isNotEmpty).distinct().joinToString(",")

android {
    namespace = "dev.xuanran.xposed.loader.modern"
    defaultConfig {
        buildConfigField("String", "XPOSED_TARGET_PACKAGES", "\"$xposedTargetPackages\"")
    }
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation(projects.core.api)
    implementation(projects.core.runtime)
    implementation(projects.loader.startup)
    compileOnly(libs.libxposed.api)
    testImplementation(libs.libxposed.api)
    testImplementation(libs.junit4)
}
