package ru.urent.qatools.data_qr_code.impl

import org.koin.dsl.module
import ru.urent.qatools.core_database.DatabaseDriverProvider
import ru.urent.qatools.data_qr_code.QrCodeRepository
import ru.urent.qatools.data_qr_code.db.QrCodeDatabase

class DataQrCodeModule {
    val module = module {
        single { QrCodeDatabase(get<DatabaseDriverProvider>().driver(QR_CODE_DATABASE_FILE, QrCodeDatabase.Schema)) }
        single<QrCodeRepository> { QrCodeRepositoryImpl(database = get()) }
    }

    private companion object {
        const val QR_CODE_DATABASE_FILE = "qr-code.db"
    }
}
