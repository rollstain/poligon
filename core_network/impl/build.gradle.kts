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
            implementation(project(":core_network:api"))
            implementation(project(":core_logger:api"))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.koin.core)
        }
        getByName("desktopMain") {
            dependencies {
                implementation(libs.ktor.client.java)
            }
        }
        getByName("desktopTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":core_entities"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
                implementation(project(":testing_stand"))
            }
        }
    }
}
