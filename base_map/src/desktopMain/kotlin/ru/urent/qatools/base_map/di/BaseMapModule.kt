package ru.urent.qatools.base_map.di

import io.ktor.client.plugins.cache.storage.FileStorage
import java.io.File
import kotlinx.io.files.Path
import org.koin.dsl.module
import ru.urent.qatools.base_map.MapTileLoader
import ru.urent.qatools.core_network.HttpClientFactory
import ru.urent.qatools.core_platform.AppFiles

class BaseMapModule {
    val module = module {
        single {
            val tileCacheDirectory = File(get<AppFiles>().root, TILE_CACHE_FOLDER)
            tileCacheDirectory.mkdirs()
            MapTileLoader(
                httpClient = get<HttpClientFactory>().createCaching(storage = FileStorage(Path(tileCacheDirectory.path))),
                log = get(),
            )
        }
    }

    private companion object {
        const val TILE_CACHE_FOLDER = "tiles"
    }
}
