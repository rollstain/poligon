package ru.urent.qatools.core_network.impl

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import kotlin.time.TimeSource
import kotlinx.io.IOException
import ru.urent.qatools.core_logger.AppLog
import ru.urent.qatools.core_network.SkipRequestLog

internal class RequestLoggingConfig {
    lateinit var log: AppLog
}

private const val HIDDEN_VALUE = "***"
private val SecretParameterMarkers = listOf("key", "token", "secret", "password")

internal val RequestLogging = createClientPlugin("RequestLogging", ::RequestLoggingConfig) {
    val log = pluginConfig.log
    on(Send) { request ->
        if (request.attributes.contains(SkipRequestLog)) return@on proceed(request)
        val description = "${request.method.value} ${describeUrl(request)}"
        val startedAt = TimeSource.Monotonic.markNow()
        try {
            val call = proceed(request)
            log.info("$description → ${call.response.status.value} (${startedAt.elapsedNow().inWholeMilliseconds} мс)")
            call
        } catch (exception: IOException) {
            log.error("$description → нет ответа (${startedAt.elapsedNow().inWholeMilliseconds} мс)", exception)
            throw exception
        }
    }
}

private fun describeUrl(request: HttpRequestBuilder): String {
    val url = request.url.build()
    val parameters = url.parameters
    val query = mutableListOf<String>()
    for (name in parameters.names().sorted()) {
        val isSecret = SecretParameterMarkers.any { marker -> name.contains(marker, ignoreCase = true) }
        for (value in parameters.getAll(name).orEmpty()) {
            query.add("$name=${if (isSecret) HIDDEN_VALUE else value}")
        }
    }
    val path = "${url.host}${url.encodedPath}"
    return if (query.isEmpty()) path else "$path?${query.joinToString("&")}"
}
