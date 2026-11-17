plugins {
    id("pfm.android.library")
    id("pfm.android.hilt")
}

android {
    namespace = "hopper.pfm.insect.core.data"
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
