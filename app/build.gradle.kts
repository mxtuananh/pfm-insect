plugins {
    id("pfm.android.application")
    id("pfm.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "hopper.pfm.insect"

    defaultConfig {
        applicationId = "hopper.pfm.insect"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures { buildConfig = true }

    buildTypes {
        debug {
            // The dev server is reached through "adb reverse tcp:8080 tcp:8080" (see docs/SERVER.md).
            buildConfigField("String", "API_BASE_URL", "\"http://localhost:8080\"")
        }
        release {
            // Override with -Ppfm.apiBaseUrl=https://... for a real deployment (HTTPS only).
            val url = providers.gradleProperty("pfm.apiBaseUrl").getOrElse("https://pfm.invalid")
            buildConfigField("String", "API_BASE_URL", "\"$url\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:category"))
    implementation(project(":feature:expense"))
    implementation(project(":feature:habit"))
    implementation(project(":feature:budget"))
    implementation(project(":feature:report"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.lifecycle.process)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
