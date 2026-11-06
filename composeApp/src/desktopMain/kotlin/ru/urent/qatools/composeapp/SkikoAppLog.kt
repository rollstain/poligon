package ru.urent.qatools.composeapp

import org.jetbrains.skiko.SkikoLoggerInterface
import ru.urent.qatools.core_logger.AppLog

internal class SkikoAppLog(private val log: AppLog) : SkikoLoggerInterface {
    override val isTraceEnabled = false

    override val isDebugEnabled = false

    override val isInfoEnabled = true

    override val isWarnEnabled = true

    override val isErrorEnabled = true

    override fun trace(message: String) = Unit

    override fun debug(message: String) = Unit

    override fun info(message: String) = log.info("$PREFIX$message")

    override fun warn(message: String) = log.error("$PREFIX$message", cause = null)

    override fun error(message: String) = log.error("$PREFIX$message", cause = null)

    override fun trace(t: Throwable, message: String) = Unit

    override fun debug(t: Throwable, message: String) = Unit

    override fun info(t: Throwable, message: String) = log.info("$PREFIX$message\n${t.stackTraceToString()}")

    override fun warn(t: Throwable, message: String) = log.error("$PREFIX$message", t)

    override fun error(t: Throwable, message: String) = log.error("$PREFIX$message", t)

    private companion object {
        const val PREFIX = "Skiko: "
    }
}
