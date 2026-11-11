package ru.urent.qatools.data_simulator.impl

import org.koin.dsl.module
import ru.urent.qatools.core_network.HttpClientFactory
import ru.urent.qatools.core_network.StandRejectionListener
import ru.urent.qatools.data_simulator.OmniEmulatorRepository
import ru.urent.qatools.data_simulator.SimulatorRepository
import ru.urent.qatools.data_simulator.VoiEmulatorRepository

class DataSimulatorModule {
    val module = module {
        single<SimulatorRepository> {
            val rejectionListener = get<StandRejectionListener>()
            SimulatorRepositoryImpl(
                connection = get<HttpClientFactory>().createStandFacing(onRejected = rejectionListener::onStandRejected),
            )
        }
        single<OmniEmulatorRepository> {
            val rejectionListener = get<StandRejectionListener>()
            OmniEmulatorRepositoryImpl(
                connection = get<HttpClientFactory>().createStandFacing(onRejected = rejectionListener::onStandRejected),
            )
        }
        single<VoiEmulatorRepository> {
            val rejectionListener = get<StandRejectionListener>()
            VoiEmulatorRepositoryImpl(
                connection = get<HttpClientFactory>().createStandFacing(onRejected = rejectionListener::onStandRejected),
            )
        }
    }
}
