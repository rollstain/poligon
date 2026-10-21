package ru.urent.qatools.composeapp.di

import com.arkivanov.decompose.ComponentContext
import org.koin.dsl.module
import ru.urent.qatools.base_bike_setup.di.BaseBikeSetupModule
import ru.urent.qatools.base_map.di.BaseMapModule
import ru.urent.qatools.base_stand.BaseStandModule
import ru.urent.qatools.composeapp.root.ScreenFactory
import ru.urent.qatools.composeapp.shell.mvi.ShellScreenModel
import ru.urent.qatools.core_database.CoreDatabaseModule
import ru.urent.qatools.core_logger.impl.CoreLoggerModule
import ru.urent.qatools.core_network.impl.CoreNetworkModule
import ru.urent.qatools.core_platform.CorePlatformModule
import ru.urent.qatools.core_settings.impl.CoreSettingsModule
import ru.urent.qatools.data_bikes.impl.DataBikesModule
import ru.urent.qatools.data_location.impl.DataLocationModule
import ru.urent.qatools.data_qr_code.impl.DataQrCodeModule
import ru.urent.qatools.data_simulator.impl.DataSimulatorModule
import ru.urent.qatools.data_stand.impl.DataStandModule
import ru.urent.qatools.data_templates.impl.DataTemplatesModule
import ru.urent.qatools.feature_login.di.FeatureLoginModule
import ru.urent.qatools.feature_qr_code.di.FeatureQrCodeModule
import ru.urent.qatools.feature_quick_setup.di.FeatureQuickSetupModule
import ru.urent.qatools.feature_settings.di.FeatureSettingsModule
import ru.urent.qatools.feature_templates.di.FeatureTemplatesModule

class AppModule {
    val module = module {
        single { ScreenFactory(koin = getKoin()) }
        factory { (context: ComponentContext) ->
            ShellScreenModel(componentContext = context, sessionInteractor = get(), appSettingsRepository = get(), log = get())
        }
    }

    val modules = listOf(
        CorePlatformModule().module,
        CoreLoggerModule().module,
        CoreDatabaseModule().module,
        CoreSettingsModule().module,
        CoreNetworkModule().module,
        DataStandModule().module,
        DataSimulatorModule().module,
        DataBikesModule().module,
        DataLocationModule().module,
        DataTemplatesModule().module,
        DataQrCodeModule().module,
        BaseStandModule().module,
        BaseBikeSetupModule().module,
        BaseMapModule().module,
        FeatureLoginModule().module,
        FeatureTemplatesModule().module,
        FeatureQuickSetupModule().module,
        FeatureSettingsModule().module,
        FeatureQrCodeModule().module,
        module,
    )
}
