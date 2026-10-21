package ru.urent.qatools.feature_settings.domain

import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.urent.qatools.core_logger.AppLog
import ru.urent.qatools.core_platform.AppFiles
import ru.urent.qatools.core_platform.AppInfo
import ru.urent.qatools.feature_settings.entities.LogArchiveResult

class SettingsInteractor(private val appFiles: AppFiles, private val log: AppLog) {
    val dataDirectory: File
        get() = appFiles.root

    val logsDirectory: File
        get() = appFiles.logs

    fun openFolder(directory: File) {
        directory.mkdirs()
        Desktop.getDesktop().open(directory)
    }

    suspend fun saveLogArchive(target: File, standHost: String): LogArchiveResult {
        log.info("Сохраняем архив логов: ${target.name}")
        return try {
            withContext(Dispatchers.IO) { writeLogArchive(target = target, standHost = standHost) }
            LogArchiveResult.Saved(path = target.absolutePath)
        } catch (exception: IOException) {
            log.error("Не удалось сохранить архив логов", exception)
            LogArchiveResult.Failed(reason = exception.message.orEmpty())
        }
    }

    private fun writeLogArchive(target: File, standHost: String) {
        val details = listOf(
            "${AppInfo.APP_NAME} ${AppInfo.version}",
            AppInfo.systemDescription,
            "Стенд: $standHost",
            "Архив собран: ${LocalDateTime.now()}",
        ).joinToString(separator = "\n", postfix = "\n")
        val logFiles = appFiles.logs.listFiles().orEmpty().filter { it.isFile }.sortedBy { it.name }
        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(DETAILS_ENTRY_NAME))
            zip.write(details.encodeToByteArray())
            zip.closeEntry()
            for (file in logFiles) {
                zip.putNextEntry(ZipEntry("$LOGS_ENTRY_FOLDER/${file.name}"))
                file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    private companion object {
        const val DETAILS_ENTRY_NAME = "details.txt"
        const val LOGS_ENTRY_FOLDER = "logs"
    }
}
