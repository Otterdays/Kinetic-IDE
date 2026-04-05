pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "9.1.0"
        id("org.jetbrains.kotlin.plugin.compose") version "2.3.10"
        id("com.google.devtools.ksp") version "2.3.6"
        id("com.google.dagger.hilt.android") version "2.59.2"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Kinetic"
include(":app")
