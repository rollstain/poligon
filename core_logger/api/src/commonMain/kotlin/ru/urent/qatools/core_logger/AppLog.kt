package ru.urent.qatools.core_logger

interface AppLog {
    fun info(message: String)

    fun error(message: String, cause: Throwable?)
}
