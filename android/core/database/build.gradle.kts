plugins {
    alias(libs.plugins.tripboard.android.library)
    alias(libs.plugins.tripboard.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.tw0winju.tripboard.core.database"
}

room {
    // 스키마 JSON을 커밋해 두면 마이그레이션 테스트/리뷰에 쓸 수 있다.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
