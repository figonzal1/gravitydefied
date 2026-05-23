pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// foojay toolchain resolver removed — using system JDK 21
// plugins {
//     id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
// }

rootProject.name = "GravityDefied"
include(":app")
