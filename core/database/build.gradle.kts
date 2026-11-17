plugins {
    id("pfm.android.library")
    id("pfm.android.hilt")
}

android {
    namespace = "hopper.pfm.insect.core.database"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
