plugins {
    id("xposed.android.library")
}

android {
    namespace = "dev.xuanran.xposed.startup"
}

dependencies {
    implementation(projects.core.api)
    implementation(projects.core.runtime)
    implementation(projects.core.dexkit)
}
