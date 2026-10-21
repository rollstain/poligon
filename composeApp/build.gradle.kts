import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
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

version = "1.0.0"
val distributionName = "Poligon"

kotlin {
    jvm("desktop")
    jvmToolchain(17)

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.components.resources)
        }
        getByName("desktopMain") {
            dependencies {
                implementation(project(":core_base_feature"))
                implementation(project(":core_database"))
                implementation(project(":core_logger:api"))
                implementation(project(":core_logger:impl"))
                implementation(project(":core_network:impl"))
                implementation(project(":core_platform"))
                implementation(project(":core_settings:api"))
                implementation(project(":core_settings:impl"))
                implementation(project(":data_bikes:impl"))
                implementation(project(":data_location:impl"))
                implementation(project(":data_qr_code:impl"))
                implementation(project(":data_simulator:impl"))
                implementation(project(":data_stand:impl"))
                implementation(project(":data_templates:impl"))
                implementation(project(":base_stand"))
                implementation(project(":base_bike_setup"))
                implementation(project(":base_map"))
                implementation(project(":feature_login"))
                implementation(project(":feature_qr_code"))
                implementation(project(":feature_quick_setup"))
                implementation(project(":feature_settings"))
                implementation(project(":feature_templates"))
                implementation(project(":navigation"))
                implementation(project(":uikit"))
                implementation(compose.desktop.currentOs)
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.decompose)
                implementation(libs.decompose.extensions.compose)
                implementation(libs.koin.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "ru.urent.qatools.composeapp.resources"
}

compose.desktop {
    application {
        mainClass = "ru.urent.qatools.composeapp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = distributionName
            packageVersion = version.toString()
            description = "Poligon"
            vendor = "Urent"
            modules("java.net.http", "java.sql", "jdk.crypto.ec", "jdk.unsupported")
            windows {
                menuGroup = "Urent"
                upgradeUuid = "8a3c1f52-6f1e-4c2b-9d7a-3e5b2c4f6a81"
                iconFile.set(project.file("icons/poligon.ico"))
            }
        }
    }
}

val syncPoligonFolder = tasks.register<Sync>("syncPoligonFolder") {
    val createDistributable = tasks.named<AbstractJPackageTask>("createDistributable")
    from(createDistributable.flatMap { it.destinationDir.dir(distributionName) })
    into(rootProject.layout.projectDirectory.dir(distributionName))
}

tasks.register<Zip>("packageZip") {
    from(syncPoligonFolder)
    archiveBaseName.set(distributionName)
    destinationDirectory.set(rootProject.layout.projectDirectory)
}
