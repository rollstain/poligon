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
            implementation(project(":core_logger:api"))
            implementation(project(":core_network:api"))
            implementation(project(":core_platform"))
            implementation(project(":data_stand:api"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.koin.core)
        }
        getByName("desktopTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":core_network:impl"))
                implementation(project(":core_database"))
                implementation(project(":data_stand:impl"))
                implementation(project(":testing_stand"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}
