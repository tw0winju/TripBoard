plugins {
    alias(libs.plugins.tripboard.android.library)
    alias(libs.plugins.tripboard.android.compose)
    alias(libs.plugins.tripboard.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.tw0winju.tripboard.feature.trips"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.designsystem)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.coroutines.test)
}
