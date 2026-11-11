package ru.urent.qatools.base_update.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module
import ru.urent.qatools.base_update.domain.MsiInstallLauncher
import ru.urent.qatools.base_update.domain.UpdateInteractor
import ru.urent.qatools.core_platform.AppInfo

class BaseUpdateModule {
    val module = module {
        single { MsiInstallLauncher(appFiles = get(), launcherPath = AppInfo.launcherPath, log = get()) }
        single {
            UpdateInteractor(
                releasesRepository = get(),
                installedVersion = AppInfo.installedVersion,
                appFiles = get(),
                installLauncher = get(),
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                log = get(),
            )
        }
    }
}
