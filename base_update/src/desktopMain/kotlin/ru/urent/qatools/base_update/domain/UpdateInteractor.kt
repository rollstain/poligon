package ru.urent.qatools.base_update.domain

import java.io.File
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.HexFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.urent.qatools.base_update.entities.ReleaseInstaller
import ru.urent.qatools.base_update.entities.UpdateStatus
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.core_logger.AppLog
import ru.urent.qatools.core_platform.AppFiles
import ru.urent.qatools.data_releases.ReleasesRepository

class UpdateInteractor(
    private val releasesRepository: ReleasesRepository,
    private val installedVersion: String?,
    private val appFiles: AppFiles,
    private val installLauncher: MsiInstallLauncher,
    private val scope: CoroutineScope,
    private val log: AppLog,
) {
    private val mutableStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.NotChecked)

    val status: StateFlow<UpdateStatus> = mutableStatus.asStateFlow()

    suspend fun deleteDownloadedInstallers() {
        withContext(Dispatchers.IO) {
            for (file in appFiles.updates.listFiles().orEmpty()) file.delete()
        }
    }

    fun check() {
        val currentVersion = installedVersion
        if (currentVersion == null) {
            mutableStatus.value = UpdateStatus.DevelopmentBuild
            return
        }
        mutableStatus.value = UpdateStatus.Checking
        scope.launch { mutableStatus.value = statusAfterCheck(currentVersion) }
    }

    fun download() {
        val installer = when (val current = mutableStatus.value) {
            is UpdateStatus.Available -> current.installer
            is UpdateStatus.DownloadFailed -> current.installer
            else -> return
        }
        mutableStatus.value = UpdateStatus.Downloading(installer = installer, percent = 0)
        scope.launch { mutableStatus.value = statusAfterDownload(installer) }
    }

    suspend fun install(): Boolean {
        val downloaded = mutableStatus.value as? UpdateStatus.Downloaded ?: return false
        mutableStatus.value = UpdateStatus.Installing(downloaded.installer)
        val isLaunched = installLauncher.launch(installer = downloaded.file, version = downloaded.installer.version)
        if (!isLaunched) mutableStatus.value = UpdateStatus.InstallFailed(installer = downloaded.installer, file = downloaded.file)
        return isLaunched
    }

    private suspend fun statusAfterCheck(currentVersion: String): UpdateStatus {
        val release = when (val loaded = releasesRepository.getLatestRelease()) {
            is RequestResult.Error -> {
                log.error("Не удалось проверить обновления: ${loaded.failure}", cause = null)
                return UpdateStatus.CheckFailed
            }
            is RequestResult.Success -> loaded.data
        }
        val version = release.tagName?.removePrefix(RELEASE_TAG_PREFIX)
        val releaseParts = if (version == null) null else versionPartsOf(version)
        val installedParts = versionPartsOf(currentVersion)
        if (version == null || releaseParts == null || installedParts == null) {
            log.error("Не удалось сравнить версии: на GitHub ${release.tagName}, установлена $currentVersion", cause = null)
            return UpdateStatus.CheckFailed
        }
        if (!isNewer(releaseParts = releaseParts, installedParts = installedParts)) {
            log.info("Обновлений нет: установлена $currentVersion, последняя на GitHub $version")
            return UpdateStatus.UpToDate
        }
        val fileName = "$INSTALLER_NAME_PREFIX$version$INSTALLER_EXTENSION"
        val asset = release.assets.orEmpty().firstOrNull { it.name == fileName }
        val url = asset?.browserDownloadUrl
        val sizeBytes = asset?.size
        val digest = asset?.digest
        if (url == null || sizeBytes == null || sizeBytes <= 0 || digest == null || !digest.startsWith(SHA256_DIGEST_PREFIX)) {
            log.error("В релизе $version нет $fileName с размером и SHA-256", cause = null)
            return UpdateStatus.CheckFailed
        }
        log.info("Доступна версия $version, установлена $currentVersion")
        val installer = ReleaseInstaller(
            version = version,
            fileName = fileName,
            url = url,
            sizeBytes = sizeBytes,
            sha256 = digest.removePrefix(SHA256_DIGEST_PREFIX),
        )
        return UpdateStatus.Available(installer)
    }

    private suspend fun statusAfterDownload(installer: ReleaseInstaller): UpdateStatus {
        val target = File(appFiles.updates, installer.fileName)
        withContext(Dispatchers.IO) { appFiles.updates.mkdirs() }
        log.info("Скачиваем ${installer.url}")
        val downloaded = releasesRepository.downloadAsset(url = installer.url, target = target) { downloadedBytes ->
            val percent = (downloadedBytes * PERCENT_IN_UNIT / installer.sizeBytes).toInt()
            mutableStatus.value = UpdateStatus.Downloading(installer = installer, percent = percent)
        }
        if (downloaded is RequestResult.Error) {
            log.error("Не удалось скачать ${installer.fileName}: ${downloaded.failure}", cause = null)
            return UpdateStatus.DownloadFailed(installer)
        }
        val sizeBytes = withContext(Dispatchers.IO) { target.length() }
        val sha256 = withContext(Dispatchers.IO) { sha256Of(target) }
        val isIntact = sizeBytes == installer.sizeBytes && sha256.equals(installer.sha256, ignoreCase = true)
        if (!isIntact) {
            log.error(
                "${installer.fileName} скачан с ошибкой: $sizeBytes байт, SHA-256 $sha256; " +
                    "в релизе ${installer.sizeBytes} байт, SHA-256 ${installer.sha256}",
                cause = null,
            )
            withContext(Dispatchers.IO) { target.delete() }
            return UpdateStatus.DownloadFailed(installer)
        }
        log.info("${installer.fileName} скачан, размер и SHA-256 совпали с релизом")
        return UpdateStatus.Downloaded(installer = installer, file = target)
    }

    private fun versionPartsOf(version: String): List<Int>? {
        val parts = mutableListOf<Int>()
        for (part in version.split(VERSION_SEPARATOR)) {
            parts.add(part.toIntOrNull() ?: return null)
        }
        return parts
    }

    private fun isNewer(releaseParts: List<Int>, installedParts: List<Int>): Boolean {
        for (index in 0 until maxOf(releaseParts.size, installedParts.size)) {
            val releasePart = releaseParts.getOrElse(index) { MISSING_VERSION_PART }
            val installedPart = installedParts.getOrElse(index) { MISSING_VERSION_PART }
            if (releasePart != installedPart) return releasePart > installedPart
        }
        return false
    }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance(SHA256_ALGORITHM)
        DigestInputStream(file.inputStream(), digest).use { input -> input.copyTo(OutputStream.nullOutputStream()) }
        return HexFormat.of().formatHex(digest.digest())
    }

    private companion object {
        const val RELEASE_TAG_PREFIX = "v"
        const val VERSION_SEPARATOR = '.'
        const val MISSING_VERSION_PART = 0
        const val INSTALLER_NAME_PREFIX = "Poligon-"
        const val INSTALLER_EXTENSION = ".msi"
        const val SHA256_DIGEST_PREFIX = "sha256:"
        const val SHA256_ALGORITHM = "SHA-256"
        const val PERCENT_IN_UNIT = 100
    }
}
