plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
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
            implementation(project(":core_logger:api"))
            implementation(project(":core_network:api"))
            implementation(project(":data_bikes:api"))
            implementation(project(":data_location:api"))
            implementation(project(":data_simulator:api"))
            implementation(project(":data_templates:api"))
            implementation(project(":base_stand"))
            implementation(project(":uikit"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
        }
        getByName("desktopTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":testing_stand"))
                implementation(project(":core_database"))
                implementation(project(":data_templates:impl"))
                implementation(libs.compose.ui.test)
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
            }
        }
    }
}
