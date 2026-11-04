import com.tw0winju.tripboard.buildlogic.configureKotlinJvm
import com.tw0winju.tripboard.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Android 의존성이 없는 순수 Kotlin 모듈. 나중에 KMP commonMain으로 옮기기 쉬운 후보들. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        configureKotlinJvm()

        dependencies {
            add("testImplementation", libs.findLibrary("junit4").get())
            add("testImplementation", libs.findLibrary("kotlin-test").get())
        }
    }
}
