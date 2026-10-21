package ru.urent.qatools.core_platform

import java.io.File

class AppFiles(val root: File) {
    val logs: File = File(root, LOGS_FOLDER)

    private companion object {
        const val LOGS_FOLDER = "logs"
    }
}
