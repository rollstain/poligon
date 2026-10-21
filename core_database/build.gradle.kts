plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.sqldelight)
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
            implementation(project(":core_platform"))
            implementation(libs.koin.core)
        }
        getByName("desktopMain") {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}

sqldelight {
    databases {
        create("CoreDatabase") {
            packageName.set("ru.urent.qatools.core_database")
        }
    }
}
