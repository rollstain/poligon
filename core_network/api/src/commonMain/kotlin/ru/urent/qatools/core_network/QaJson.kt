package ru.urent.qatools.core_network

import kotlinx.serialization.json.Json

val QaJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}
