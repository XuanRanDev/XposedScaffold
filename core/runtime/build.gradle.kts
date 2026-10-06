plugins {
    id("xposed.android.library")
}

android {
    namespace = "dev.xuanran.xposed.runtime"
}

dependencies {
    api(projects.core.api)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit4)
}
