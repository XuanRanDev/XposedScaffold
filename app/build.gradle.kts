import java.time.Instant

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val xposedTargetPackage = providers.gradleProperty("xposedTargetPackage").get()
val xposedModulePackage = providers.gradleProperty("xposedModulePackage").get()
val xposedModuleName = providers.gradleProperty("xposedModuleName").get()
val buildTime = Instant.now().toString()
require(xposedTargetPackage.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))) {
    "xposedTargetPackage must be a valid Android package name"
}

android {
    namespace = "dev.xuanran.xposedscaffold"
    compileSdk = 37

    defaultConfig {
        applicationId = xposedModulePackage
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "XPOSED_TARGET_PACKAGE", "\"$xposedTargetPackage\"")
        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
        resValue("string", "xposed_target_package", xposedTargetPackage)
        resValue("string", "app_name", xposedModuleName)

    }

    // One APK must expose exactly one Xposed entry ABI. Mixing API 82 assets with
    // libxposed 101/102 metadata lets incompatible framework versions select the
    // wrong entry class before our code has any chance to perform a version check.
    flavorDimensions += "xposedApi"
    productFlavors {
        create("legacy") {
            dimension = "xposedApi"
            versionNameSuffix = "-legacy"
            buildConfigField("String", "XPOSED_API_LABEL", "\"Legacy API 82\"")
        }
        create("modern") {
            dimension = "xposedApi"
            versionNameSuffix = "-modern"
            buildConfigField("String", "XPOSED_API_LABEL", "\"libxposed API 101–102\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging.resources {
        merges += "META-INF/xposed/*"
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    // 稳定业务接口与运行时，不依赖具体 Xposed 框架实现。
    implementation(projects.core.api)
    implementation(projects.core.runtime)
    implementation(projects.core.dexkit)
    // Shared startup is framework-neutral; each flavor packages only its matching loader.
    implementation(projects.loader.startup)
    add("legacyImplementation", projects.loader.legacy)
    add("modernImplementation", projects.loader.modern)
    add("modernImplementation", libs.libxposed.service)
    // 编译期生成 Hook 注册表，杜绝运行时扫描整个 DEX。
    ksp(projects.processor)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
