rootProject.name = "urent-qa-tools"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

include(":core_entities")
include(":core_logger:api")
include(":core_logger:impl")
include(":core_platform")
include(":core_database")
include(":core_settings:api")
include(":core_settings:impl")
include(":core_network:api")
include(":core_network:impl")
include(":core_base_feature")
include(":uikit")
include(":navigation")

include(":data_stand:api")
include(":data_stand:impl")
include(":data_simulator:api")
include(":data_simulator:impl")
include(":data_bikes:api")
include(":data_bikes:impl")
include(":data_location:api")
include(":data_location:impl")
include(":data_templates:api")
include(":data_templates:impl")
include(":data_qr_code:api")
include(":data_qr_code:impl")
include(":data_releases:api")
include(":data_releases:impl")

include(":base_stand")
include(":base_bike_setup")
include(":base_map")
include(":base_update")

include(":feature_login")
include(":feature_templates")
include(":feature_quick_setup")
include(":feature_settings")
include(":feature_qr_code")

include(":composeApp")

include(":testing_stand")
