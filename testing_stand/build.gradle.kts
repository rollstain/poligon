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
        getByName("desktopMain") {
            dependencies {
                implementation(project(":core_database"))
                implementation(project(":core_entities"))
                implementation(project(":core_logger:api"))
                implementation(project(":core_network:api"))
                implementation(project(":core_network:impl"))
                implementation(project(":data_bikes:api"))
                implementation(project(":data_location:api"))
                implementation(project(":data_simulator:api"))
                implementation(project(":data_templates:api"))
                implementation(project(":data_bikes:impl"))
                implementation(project(":data_location:impl"))
                implementation(project(":data_simulator:impl"))
                implementation(project(":data_templates:impl"))
                implementation(project(":base_bike_setup"))
                implementation(project(":base_map"))
                implementation(project(":navigation"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.mock)
                implementation(libs.koin.core)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}
