package ru.urent.qatools.data_opstask.impl

import org.koin.dsl.module
import ru.urent.qatools.data_opstask.OpsTaskRepository

class DataOpsTaskModule {
    val module = module {
        single<OpsTaskRepository> { OpsTaskRepositoryImpl(clientProvider = get()) }
    }
}
