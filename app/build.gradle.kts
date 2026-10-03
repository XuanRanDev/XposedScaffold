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
    implementation(projects.core.api)
    implementation(projects.core.runtime)
    implementation(projects.core.dexkit)
    implementation(projects.loader.startup)
    implementation(projects.loader.legacy)
    implementation(projects.loader.modern)
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
