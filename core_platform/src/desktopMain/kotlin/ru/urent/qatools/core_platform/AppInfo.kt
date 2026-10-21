package ru.urent.qatools.core_platform

object AppInfo {
    const val APP_NAME = "Полигон"
    private const val JPACKAGE_VERSION_PROPERTY = "jpackage.app-version"
    private const val DEVELOPMENT_VERSION = "сборка разработчика"

    val version: String
        get() = System.getProperty(JPACKAGE_VERSION_PROPERTY) ?: DEVELOPMENT_VERSION

    val systemDescription: String
        get() = "${System.getProperty("os.name")} ${System.getProperty("os.version")} · Java ${System.getProperty("java.version")}"
}
