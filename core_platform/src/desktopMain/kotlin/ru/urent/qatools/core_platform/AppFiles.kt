package ru.urent.qatools.core_platform

import java.io.File

class AppFiles(val root: File) {
    val logs: File = File(root, LOGS_FOLDER)
    val updates: File = File(root, UPDATES_FOLDER)

    private companion object {
        const val LOGS_FOLDER = "logs"
        const val UPDATES_FOLDER = "updates"
    }
}
