plugins {
    id("pfm.android.library.compose")
}

android {
    namespace = "hopper.pfm.insect.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    api(libs.dev.chrisbanes.haze)
    api(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
