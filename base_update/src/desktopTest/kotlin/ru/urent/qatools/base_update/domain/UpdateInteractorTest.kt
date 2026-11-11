package ru.urent.qatools.base_update.domain

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.File
import java.security.MessageDigest
import java.util.HexFormat
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import ru.urent.qatools.base_update.entities.ReleaseInstaller
import ru.urent.qatools.base_update.entities.UpdateStatus
import ru.urent.qatools.core_network.HttpClientFactory
import ru.urent.qatools.core_network.impl.HttpClientFactoryImpl
import ru.urent.qatools.core_platform.AppFiles
import ru.urent.qatools.data_releases.ReleasesRepository
import ru.urent.qatools.data_releases.impl.DataReleasesModule
import ru.urent.qatools.testing_stand.SilentLog
import ru.urent.qatools.testing_stand.TestEngineFactory

class UpdateInteractorTest {
    private val dataDirectory = createTempDirectory(TEMP_DIRECTORY_PREFIX).toFile()
    private val appFiles = AppFiles(root = dataDirectory)
    private var gitHubRequests = 0

    @AfterTest
    fun deleteDataDirectory() {
        dataDirectory.deleteRecursively()
    }

    @Test
    fun newerReleaseIsOfferedWithItsInstaller() = runBlocking {
        val interactor = interactor(scope = this, release = publishedRelease(tag = "v1.2.0"), installedVersion = "1.1.1")

        val status = checkedStatus(interactor)

        val installer = ReleaseInstaller(
            version = "1.2.0",
            fileName = "Poligon-1.2.0.msi",
            url = INSTALLER_URL,
            sizeBytes = InstallerBytes.size.toLong(),
            sha256 = sha256Of(InstallerBytes),
        )
        assertEquals(UpdateStatus.Available(installer), status)
    }

    @Test
    fun sameVersionIsUpToDate() = runBlocking {
        val interactor = interactor(scope = this, release = publishedRelease(tag = "v1.2.0"), installedVersion = "1.2.0")

        assertEquals(UpdateStatus.UpToDate, checkedStatus(interactor))
    }

    @Test
    fun olderReleaseIsNotOffered() = runBlocking {
        val interactor = interactor(scope = this, release = publishedRelease(tag = "v1.1.1"), installedVersion = "1.2.0")

        assertEquals(UpdateStatus.UpToDate, checkedStatus(interactor))
    }

    @Test
    fun versionsAreComparedAsNumbersNotAsText() = runBlocking {
        val interactor = interactor(scope = this, release = publishedRelease(tag = "v1.10.0"), installedVersion = "1.9.0")

        val status = checkedStatus(interactor)

        assertTrue(status is UpdateStatus.Available, "1.10.0 новее 1.9.0, а получили $status")
    }

    @Test
    fun releaseWithoutChecksumIsNotOffered() = runBlocking {
        val release = publishedRelease(tag = "v1.2.0", sha256 = null)
        val interactor = interactor(scope = this, release = release, installedVersion = "1.1.1")

        assertEquals(UpdateStatus.CheckFailed, checkedStatus(interactor))
    }

    @Test
    fun gitHubRefusalLeavesCheckFailed() = runBlocking {
        val interactor = interactor(scope = this, release = null, installedVersion = "1.1.1")

        assertEquals(UpdateStatus.CheckFailed, checkedStatus(interactor))
    }

    @Test
    fun developmentBuildDoesNotAskGitHub() = runBlocking {
        val interactor = interactor(scope = this, release = publishedRelease(tag = "v1.2.0"), installedVersion = null)

        interactor.check()

        assertEquals(UpdateStatus.DevelopmentBuild, interactor.status.value)
        assertEquals(0, gitHubRequests)
    }

    @Test
    fun installerMatchingReleaseIsReadyToInstall() = runBlocking {
        val interactor = interactor(scope = this, release = publishedRelease(tag = "v1.2.0"), installedVersion = "1.1.1")
        checkedStatus(interactor)

        val status = downloadedStatus(interactor)

        assertTrue(status is UpdateStatus.Downloaded, "Ожидали скачанный установщик, а получили $status")
        assertEquals(InstallerBytes.toList(), status.file.readBytes().toList())
        assertEquals(File(appFiles.updates, "Poligon-1.2.0.msi"), status.file)
    }

    @Test
    fun truncatedInstallerIsDeletedAndOfferedAgain() = runBlocking {
        val release = publishedRelease(tag = "v1.2.0", servedBytes = InstallerBytes.copyOf(TRUNCATED_INSTALLER_SIZE))
        val interactor = interactor(scope = this, release = release, installedVersion = "1.1.1")
        checkedStatus(interactor)

        val status = downloadedStatus(interactor)

        assertTrue(status is UpdateStatus.DownloadFailed, "Обрезанный файл нельзя ставить, а получили $status")
        assertFalse(File(appFiles.updates, "Poligon-1.2.0.msi").exists())
    }

    @Test
    fun replacedInstallerOfSameSizeIsRejected() = runBlocking {
        val release = publishedRelease(tag = "v1.2.0", servedBytes = InstallerBytes.reversedArray())
        val interactor = interactor(scope = this, release = release, installedVersion = "1.1.1")
        checkedStatus(interactor)

        val status = downloadedStatus(interactor)

        assertTrue(status is UpdateStatus.DownloadFailed, "Файл с чужим SHA-256 нельзя ставить, а получили $status")
    }

    @Test
    fun installersLeftByPreviousUpdateAreDeleted() = runBlocking {
        val interactor = interactor(scope = this, release = publishedRelease(tag = "v1.2.0"), installedVersion = "1.2.0")
        appFiles.updates.mkdirs()
        val leftover = File(appFiles.updates, "Poligon-1.1.1.msi")
        leftover.writeBytes(InstallerBytes)

        interactor.deleteDownloadedInstallers()

        assertFalse(leftover.exists())
    }

    private fun interactor(scope: CoroutineScope, release: PublishedRelease?, installedVersion: String?): UpdateInteractor {
        val engine = MockEngine { request ->
            if (request.url.host == GITHUB_API_HOST) {
                gitHubRequests++
                if (release == null) {
                    respond(content = RATE_LIMIT_BODY, status = HttpStatusCode.Forbidden, headers = JsonHeaders)
                } else {
                    respond(content = releaseJson(release), status = HttpStatusCode.OK, headers = JsonHeaders)
                }
            } else {
                respond(content = checkNotNull(release) { "Установщик запрошен без релиза" }.servedBytes, status = HttpStatusCode.OK)
            }
        }
        val clientFactory = HttpClientFactoryImpl(
            engineFactory = TestEngineFactory { engine },
            cachingEngineFactory = TestEngineFactory { engine },
            log = SilentLog,
        )
        val koin = koinApplication {
            modules(DataReleasesModule().module, module { single<HttpClientFactory> { clientFactory } })
        }.koin
        return UpdateInteractor(
            releasesRepository = koin.get<ReleasesRepository>(),
            installedVersion = installedVersion,
            appFiles = appFiles,
            installLauncher = MsiInstallLauncher(appFiles = appFiles, launcherPath = null, log = SilentLog),
            scope = scope,
            log = SilentLog,
        )
    }

    private suspend fun checkedStatus(interactor: UpdateInteractor): UpdateStatus {
        interactor.check()
        return withTimeout(WAIT_TIMEOUT_MS) { interactor.status.first { it != UpdateStatus.Checking } }
    }

    private suspend fun downloadedStatus(interactor: UpdateInteractor): UpdateStatus {
        interactor.download()
        return withTimeout(WAIT_TIMEOUT_MS) { interactor.status.first { it !is UpdateStatus.Downloading } }
    }

    private fun publishedRelease(tag: String, sha256: String? = sha256Of(InstallerBytes), servedBytes: ByteArray = InstallerBytes) =
        PublishedRelease(tag = tag, sizeBytes = InstallerBytes.size.toLong(), sha256 = sha256, servedBytes = servedBytes)

    private fun releaseJson(release: PublishedRelease): String {
        val version = release.tag.removePrefix("v")
        val digest = if (release.sha256 == null) "" else "\"digest\": \"sha256:${release.sha256}\","
        return """
            {
              "tag_name": "${release.tag}",
              "assets": [
                {
                  "name": "Poligon-$version.msi",
                  "size": ${release.sizeBytes},
                  $digest
                  "browser_download_url": "$INSTALLER_URL"
                }
              ]
            }
        """.trimIndent()
    }

    private fun sha256Of(bytes: ByteArray): String = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))

    private class PublishedRelease(val tag: String, val sizeBytes: Long, val sha256: String?, val servedBytes: ByteArray)

    private companion object {
        const val TEMP_DIRECTORY_PREFIX = "poligon-update-test"
        const val GITHUB_API_HOST = "api.github.com"
        const val INSTALLER_URL = "https://github.com/rollstain/poligon/releases/download/v1.2.0/Poligon.msi"
        const val RATE_LIMIT_BODY = """{"message": "API rate limit exceeded"}"""
        const val TRUNCATED_INSTALLER_SIZE = 4
        const val WAIT_TIMEOUT_MS = 5_000L
        val InstallerBytes = "Poligon MSI payload".encodeToByteArray()
        val JsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
