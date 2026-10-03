plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.xuanran.xposedscaffold"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.xuanran.xposedscaffold"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
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
    // 两套 loader 同时打包进一个 APK，由各自入口资源决定框架采用哪条路径。
    implementation(projects.loader.startup)
    implementation(projects.loader.legacy)
    implementation(projects.loader.modern)
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
