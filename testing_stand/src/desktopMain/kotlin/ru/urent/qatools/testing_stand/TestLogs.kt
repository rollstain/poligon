package ru.urent.qatools.testing_stand

import ru.urent.qatools.core_logger.AppLog

object SilentLog : AppLog {
    override fun info(message: String) = Unit

    override fun error(message: String, cause: Throwable?) = Unit
}

class RecordingLog : AppLog {
    val lines = mutableListOf<String>()

    override fun info(message: String) {
        lines.add(message)
    }

    override fun error(message: String, cause: Throwable?) {
        lines.add(message)
    }
}
