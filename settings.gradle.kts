@file:Suppress("UnstableApiUsage")

include(":Lunaris")
include(":Pixie")
include(":TablerIcons")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven("https://repo.opencollab.dev/maven-snapshots")
        maven("https://repo.opencollab.dev/maven-releases")
        maven("https://jitpack.io")
    }
}

rootProject.name = "LuminaCN"
include(":app",
    ":Protocol:bedrock-codec",
    ":Protocol:bedrock-connection",
    ":Protocol:common",
    ":Network:codec-query",
    ":Network:codec-rcon",
    ":Network:transport-raknet",
    ":minecraft-msftauth",
//    ":imgui",
    ":animatedux")
