plugins {
    alias(libs.plugins.tripboard.jvm.library)
}

// 의도적으로 의존성 없음: 동기화 알고리즘은 순수 Kotlin으로 유지한다.
// (서버와 공유하거나 KMP commonMain으로 옮길 1순위 후보)
