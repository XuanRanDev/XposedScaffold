import java.time.Instant

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// One module may target several hosts. Normalize the user-facing comma-separated property once,
// then reuse the exact same ordered list for BuildConfig and LSPosed's recommended scope resource.
val xposedTargetPackages = providers.gradleProperty("xposedTargetPackages").get()
    .split(',').map(String::trim).filter(String::isNotEmpty).distinct()
val xposedModulePackage = providers.gradleProperty("xposedModulePackage").get()
val xposedModuleName = providers.gradleProperty("xposedModuleName").get()
val buildTime = Instant.now().toString()
require(xposedTargetPackages.isNotEmpty() && xposedTargetPackages.all {
    it.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))
}) {
    "xposedTargetPackages must be a comma-separated list of valid Android package names"
}
val xposedTargetPackagesValue = xposedTargetPackages.joinToString(",")

android {
    namespace = "dev.xuanran.xposedscaffold"
    compileSdk = 37

    defaultConfig {
        applicationId = xposedModulePackage
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "XPOSED_TARGET_PACKAGES", "\"$xposedTargetPackagesValue\"")
        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
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
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
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

// `xposedscope` requires an Android string-array, while Gradle's resValue API only creates scalar
// values. Generate the array under build/ so changing gradle.properties never requires editing XML.
val generatedXposedScope = layout.buildDirectory.dir("generated/xposed-scope/res")
val generateXposedScope by tasks.registering {
    inputs.property("packages", xposedTargetPackagesValue)
    outputs.dir(generatedXposedScope)
    doLast {
        val values = generatedXposedScope.get().dir("values").asFile.apply { mkdirs() }
        val items = xposedTargetPackages.joinToString("\n") { "        <item>$it</item>" }
        values.resolve("xposed_scope.xml").writeText(
            """<resources>
    <string-array name="xposed_scope">
$items
    </string-array>
</resources>
""",
        )
    }
}

android.sourceSets["main"].res.srcDir(generatedXposedScope)
// Manifest/resource processing may start before Kotlin compilation, so make the generated resource
// an explicit preBuild dependency instead of relying on incidental task ordering.
tasks.named("preBuild").configure { dependsOn(generateXposedScope) }

kotlin { jvmToolchain(17) }

// Kotlin 2.3 enables Compose group-key deobfuscation mapping by default. It is only diagnostic
// metadata and requires an additional build-time artifact, so keep release builds reproducible
// in offline scaffold environments while retaining the normal R8 mapping.txt output.
composeCompiler {
    includeComposeMappingFile.set(false)
}

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
    debugImplementation(libs.androidx.compose.ui.tooling)
}
