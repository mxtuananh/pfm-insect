pluginManagement {
    includeBuild("build-logic")
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

rootProject.name = "pfm-insect"

include(":app")
include(":feature:auth")
include(":feature:category")
include(":feature:expense")
include(":feature:habit")
include(":feature:budget")
include(":feature:report")
include(":core:model")
include(":core:common")
include(":core:domain")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":core:designsystem")
include(":core:ui")
