package ru.urent.qatools.core_logger.impl

import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import ru.urent.qatools.core_logger.AppLog

internal class FileAppLog(private val directory: File) : AppLog {
    private val writeLock = Any()

    init {
        directory.mkdirs()
        deleteExpiredFiles()
    }

    override fun info(message: String) = write(level = INFO_LEVEL, message = message, cause = null)

    override fun error(message: String, cause: Throwable?) = write(level = ERROR_LEVEL, message = message, cause = cause)

    private fun write(level: String, message: String, cause: Throwable?) {
        val now = LocalDateTime.now()
        val stackTrace = if (cause == null) "" else "\n${cause.stackTraceToString().trimEnd()}"
        val entry = "${TimeFormat.format(now)} $level $message$stackTrace\n"
        val file = File(directory, "$FILE_PREFIX${DateFormat.format(now)}$FILE_EXTENSION")
        synchronized(writeLock) {
            try {
                file.appendText(entry)
            } catch (exception: IOException) {
                System.err.print(entry)
            }
        }
    }

    private fun deleteExpiredFiles() {
        val oldestKeptDate = LocalDate.now().minusDays(RETENTION_DAYS)
        for (file in directory.listFiles().orEmpty()) {
            val name = file.name
            if (!name.startsWith(FILE_PREFIX) || !name.endsWith(FILE_EXTENSION)) continue
            val dateText = name.removePrefix(FILE_PREFIX).removeSuffix(FILE_EXTENSION)
            val date = try {
                LocalDate.parse(dateText, DateFormat)
            } catch (exception: DateTimeParseException) {
                continue
            }
            if (date.isBefore(oldestKeptDate)) file.delete()
        }
    }

    private companion object {
        const val FILE_PREFIX = "poligon-"
        const val FILE_EXTENSION = ".log"
        const val INFO_LEVEL = "INFO "
        const val ERROR_LEVEL = "ERROR"
        const val RETENTION_DAYS = 7L
        val DateFormat: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }
}
