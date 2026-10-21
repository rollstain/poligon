package ru.urent.qatools.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screens {
    @Serializable
    data object Login : Screens

    @Serializable
    data class Templates(val standBaseUrl: String) : Screens

    @Serializable
    data class Template(val standBaseUrl: String, val templateId: Long) : Screens

    @Serializable
    data class QuickSetup(val standBaseUrl: String) : Screens

    @Serializable
    data class QrCode(val bikeQrText: String?) : Screens

    @Serializable
    data object Settings : Screens
}
