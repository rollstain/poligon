package ru.urent.qatools.base_update.entities

data class ReleaseInstaller(
    val version: String,
    val fileName: String,
    val url: String,
    val sizeBytes: Long,
    val sha256: String,
)
