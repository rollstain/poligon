package ru.urent.qatools.base_update.domain

import java.io.File
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.urent.qatools.core_logger.AppLog
import ru.urent.qatools.core_platform.AppFiles

class MsiInstallLauncher(private val appFiles: AppFiles, private val launcherPath: String?, private val log: AppLog) {

    suspend fun launch(installer: File, version: String): Boolean {
        val launcher = launcherPath
        if (launcher == null) {
            log.error("Установка $version не запущена: путь к Poligon.exe неизвестен, это не установленный Полигон", cause = null)
            return false
        }
        val installLog = File(appFiles.logs, "$INSTALL_LOG_PREFIX$version$INSTALL_LOG_EXTENSION")
        val msiexecArguments = "/i \"${installer.absolutePath}\" /passive /norestart /l*v \"${installLog.absolutePath}\""
        val installScript = listOf(
            "Wait-Process -Id ${ProcessHandle.current().pid()} -Timeout $APP_EXIT_TIMEOUT_SECONDS -ErrorAction SilentlyContinue",
            "Start-Process -FilePath msiexec.exe -ArgumentList ${powerShellLiteral(msiexecArguments)} -Wait",
            "Start-Process -FilePath ${powerShellLiteral(launcher)}",
        ).joinToString(separator = "\n")
        val outsideJobScript = listOf(
            "\$created = Invoke-CimMethod -ClassName Win32_Process -MethodName Create " +
                "-Arguments @{ CommandLine = ${powerShellLiteral(hiddenPowerShellCommand(installScript))} }",
            "exit \$created.ReturnValue",
        ).joinToString(separator = "\n")
        return withContext(Dispatchers.IO) {
            try {
                val process = ProcessBuilder(POWERSHELL, *HiddenPowerShellOptions, ENCODED_COMMAND_OPTION, encoded(outsideJobScript))
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
                val isFinished = process.waitFor(LAUNCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                if (!isFinished) process.destroy()
                val exitCode = if (isFinished) process.exitValue() else null
                if (exitCode != WMI_SUCCESS) {
                    log.error("Установка $version не запущена: WMI вернул ${exitCode ?: "таймаут"}", cause = null)
                    return@withContext false
                }
                log.info("Запущена установка $version из ${installer.absolutePath}, журнал установщика: ${installLog.absolutePath}")
                true
            } catch (exception: IOException) {
                log.error("Не удалось запустить установку $version", exception)
                false
            }
        }
    }

    private fun hiddenPowerShellCommand(script: String): String =
        listOf(POWERSHELL, *HiddenPowerShellOptions, ENCODED_COMMAND_OPTION, encoded(script)).joinToString(separator = " ")

    private fun encoded(script: String): String = Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_16LE))

    private fun powerShellLiteral(value: String): String = "'${value.replace("'", "''")}'"

    private companion object {
        const val POWERSHELL = "powershell.exe"
        const val ENCODED_COMMAND_OPTION = "-EncodedCommand"
        const val APP_EXIT_TIMEOUT_SECONDS = 60
        const val LAUNCH_TIMEOUT_SECONDS = 30L
        const val WMI_SUCCESS = 0
        const val INSTALL_LOG_PREFIX = "update-"
        const val INSTALL_LOG_EXTENSION = ".log"
        val HiddenPowerShellOptions = arrayOf("-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden")
    }
}
