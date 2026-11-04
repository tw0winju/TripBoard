plugins {
    // JDK 21이 없으면 toolchain을 자동으로 받아온다
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "tripboard-server"
