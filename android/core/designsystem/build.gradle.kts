plugins {
    alias(libs.plugins.tripboard.android.library)
    alias(libs.plugins.tripboard.android.compose)
}

android {
    namespace = "com.tw0winju.tripboard.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.material3)
}
