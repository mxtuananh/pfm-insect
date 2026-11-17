plugins {
    id("pfm.android.library")
    id("pfm.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "hopper.pfm.insect.core.network"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    api(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    api(libs.ktor.client.auth)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}
