plugins {
    alias(libs.plugins.tripboard.android.application)
    alias(libs.plugins.tripboard.android.compose)
    alias(libs.plugins.tripboard.hilt)
}

android {
    namespace = "com.tw0winju.tripboard"

    defaultConfig {
        applicationId = "com.tw0winju.tripboard"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.feature.trips)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
}
