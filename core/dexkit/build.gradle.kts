plugins {
    id("xposed.android.library")
}

android {
    namespace = "dev.xuanran.xposed.dexkit"
}

dependencies {
    api(projects.core.api)
    api(libs.dexkit)
}
