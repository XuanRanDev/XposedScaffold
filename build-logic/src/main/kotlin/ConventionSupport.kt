import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal const val COMPILE_SDK = 37
internal const val MIN_SDK = 26

internal fun Project.configureAndroid(extension: ApplicationExtension) {
    extension.compileSdk = COMPILE_SDK
    extension.defaultConfig.minSdk = MIN_SDK
    extension.compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    extensions.getByType(KotlinAndroidProjectExtension::class.java).apply {
        jvmToolchain(21)
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
}

internal fun Project.configureAndroid(extension: LibraryExtension) {
    extension.compileSdk = COMPILE_SDK
    extension.defaultConfig.minSdk = MIN_SDK
    extension.compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    extensions.getByType(KotlinAndroidProjectExtension::class.java).apply {
        jvmToolchain(21)
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
}
