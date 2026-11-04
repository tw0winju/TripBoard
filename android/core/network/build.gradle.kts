plugins {
    alias(libs.plugins.tripboard.android.library)
    alias(libs.plugins.tripboard.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.tw0winju.tripboard.core.network"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        // 에뮬레이터에서 호스트 PC의 localhost = 10.0.2.2
        buildConfigField("String", "SYNC_WS_URL", "\"ws://10.0.2.2:8080/ws/sync\"")
    }
}

dependencies {
    api(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
}
