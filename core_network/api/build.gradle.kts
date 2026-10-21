plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.detekt)
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}

detekt {
    config.setFrom("$rootDir/detekt-config.yml")
    source.setFrom("src/commonMain/kotlin", "src/desktopMain/kotlin", "build.gradle.kts")
    parallel = true
    buildUponDefaultConfig = false
}

kotlin {
    jvm("desktop")
    jvmToolchain(17)

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core_entities"))
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
        }
        getByName("desktopTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
