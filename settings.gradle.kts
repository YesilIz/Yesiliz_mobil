pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// JDK 17 makinede kurulu olmayınca (ya da kurulamayınca — antivirüs/kurumsal
// kısıt) Gradle'ın kendisi indirsin diye. Eklenti olmadan toolchain hatası
// "Toolchain download repositories have not been configured" diyerek
// derlemeyi tamamen durduruyordu.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Yesiliz"

include(":app")
include(":core-risk")
