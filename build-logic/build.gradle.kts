buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}

plugins {
    `java-gradle-plugin`
}

apply(plugin = "org.jetbrains.kotlin.jvm")

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "xposed.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "xposed.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("kotlinJvm") {
            id = "xposed.kotlin.jvm"
            implementationClass = "KotlinJvmConventionPlugin"
        }
    }
}
