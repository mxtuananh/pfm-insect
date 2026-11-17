plugins {
    id("pfm.kotlin.library")
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    testImplementation(libs.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
