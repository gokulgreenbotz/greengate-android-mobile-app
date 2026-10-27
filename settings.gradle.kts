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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // LiveKit SDK pulls its audio-routing helper (audioswitch) from JitPack.
        maven {
            url = uri("https://jitpack.io")
            content {
                includeGroup("com.github.davidliu")
            }
        }
    }
}

rootProject.name = "GreenGate"
include(":app")
 