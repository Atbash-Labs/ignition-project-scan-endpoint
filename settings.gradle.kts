pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/public")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/public")
        }
    }
    versionCatalogs {
        create("libs") {
            from(files("gradle/lib.versions.toml"))
        }
    }
}

rootProject.name = "project-scan"

include(
    ":",
    ":gateway",
    ":designer",
    ":common"
)
