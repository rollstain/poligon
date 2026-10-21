package ru.urent.qatools.base_stand

data class StandOption(val label: String, val baseUrl: String, val email: String?)

object StandOptions {
    private const val HTTPS_PREFIX = "https://"
    private const val DEV_STAND_DOMAIN = ".dev.urent.ru"
    private val devStandNames = listOf(
        "tm-0", "tm-1", "tm-2", "tm-3", "tm-4", "tm-5",
        "cs-0", "cs-1", "cs-2", "cs-3", "cs-4", "cs-5",
        "rc",
    )

    fun build(savedStands: List<Stand>): List<StandOption> {
        val emailsByUrl = savedStands.associate { it.baseUrl to it.email }
        val options = mutableListOf<StandOption>()
        for (name in devStandNames) {
            val baseUrl = "$HTTPS_PREFIX$name$DEV_STAND_DOMAIN"
            options.add(StandOption(label = name.uppercase(), baseUrl = baseUrl, email = emailsByUrl[baseUrl]))
        }
        val presetUrls = options.map { it.baseUrl }.toSet()
        for (stand in savedStands) {
            if (stand.baseUrl in presetUrls) continue
            options.add(StandOption(label = stand.baseUrl.substringAfter(HTTPS_PREFIX), baseUrl = stand.baseUrl, email = stand.email))
        }
        return options
    }
}
