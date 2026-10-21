package ru.urent.qatools.data_stand

class SavedStandEntity(
    val baseUrl: String,
    val email: String?,
    val encryptedAccessToken: ByteArray?,
    val encryptedPassword: ByteArray?,
)
