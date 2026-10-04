// 允许 build.gradle.kts 使用 projects.core.api 这类类型安全模块引用。
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 禁止子模块临时添加仓库，确保依赖来源集中、可审计。
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://api.xposed.info/") {
            content { includeGroup("de.robv.android.xposed") }
        }
    }
}

rootProject.name = providers.gradleProperty("xposedProjectName").get()

include(
    ":app",
    ":core:api",
    ":core:runtime",
    ":core:dexkit",
    ":loader:startup",
    ":loader:legacy",
    ":loader:modern",
    ":processor",
)
