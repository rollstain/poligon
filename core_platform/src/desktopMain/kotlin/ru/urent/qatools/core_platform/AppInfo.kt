package ru.urent.qatools.core_platform

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.Win32Exception
import com.sun.jna.platform.win32.WinReg

object AppInfo {
    const val APP_NAME = "Полигон"
    private const val JPACKAGE_VERSION_PROPERTY = "jpackage.app-version"
    private const val JPACKAGE_LAUNCHER_PROPERTY = "jpackage.app-path"
    private const val DEVELOPMENT_VERSION = "сборка разработчика"
    private const val DISPLAY_ADAPTERS_REGISTRY_KEY = "SYSTEM\\CurrentControlSet\\Control\\Class\\{4d36e968-e325-11ce-bfc1-08002be10318}"
    private const val ADAPTER_NAME_VALUE = "DriverDesc"
    private const val DRIVER_VERSION_VALUE = "DriverVersion"
    private const val DRIVER_DATE_VALUE = "DriverDate"
    private val AdapterRegistryKeyName = Regex("\\d{4}")

    val installedVersion: String?
        get() = System.getProperty(JPACKAGE_VERSION_PROPERTY)

    val launcherPath: String?
        get() = System.getProperty(JPACKAGE_LAUNCHER_PROPERTY)

    val version: String
        get() = installedVersion ?: DEVELOPMENT_VERSION

    val systemDescription: String
        get() = "${System.getProperty("os.name")} ${System.getProperty("os.version")} · Java ${System.getProperty("java.version")}"

    val graphicsAdapters: String
        get() = try {
            val adapters = mutableListOf<String>()
            for (keyName in Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, DISPLAY_ADAPTERS_REGISTRY_KEY)) {
                if (!AdapterRegistryKeyName.matches(keyName)) continue
                val values = Advapi32Util.registryGetValues(WinReg.HKEY_LOCAL_MACHINE, "$DISPLAY_ADAPTERS_REGISTRY_KEY\\$keyName")
                adapters.add("${values[ADAPTER_NAME_VALUE]} (драйвер ${values[DRIVER_VERSION_VALUE]} от ${values[DRIVER_DATE_VALUE]})")
            }
            adapters.joinToString("; ")
        } catch (exception: Win32Exception) {
            "не прочитаны из реестра: ${exception.message}"
        }
}
