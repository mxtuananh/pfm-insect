plugins {
    id("pfm.android.library")
    id("pfm.android.hilt")
}

android {
    namespace = "hopper.pfm.insect.core.datastore"
}

dependencies {
    api(project(":core:common"))
    api(project(":core:model"))
    api(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
}
