package ru.urent.qatools.core_network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import ru.urent.qatools.core_entities.RequestFailure
import ru.urent.qatools.core_entities.RequestResult

suspend fun <T> requestResultOf(request: suspend () -> T): RequestResult<T> =
    try {
        RequestResult.Success(request())
    } catch (exception: StandUnavailableException) {
        RequestResult.Error(RequestFailure.NoConnection)
    } catch (exception: IOException) {
        RequestResult.Error(RequestFailure.NoConnection)
    } catch (exception: SessionExpiredException) {
        RequestResult.Error(RequestFailure.SessionExpired)
    } catch (exception: UnexpectedResponseException) {
        RequestResult.Error(RequestFailure.Http(exception.statusCode))
    } catch (exception: ContentConvertException) {
        RequestResult.Error(RequestFailure.InvalidResponse)
    } catch (exception: SerializationException) {
        RequestResult.Error(RequestFailure.InvalidResponse)
    } catch (exception: IllegalStateException) {
        currentCoroutineContext().ensureActive()
        throw exception
    }

suspend inline fun <reified T> requestBodyOf(crossinline send: suspend () -> HttpResponse): RequestResult<T> =
    requestResultOf {
        val response = send()
        if (!response.status.isSuccess()) {
            throw UnexpectedResponseException(response.status.value, response.call.request.url.encodedPath)
        }
        response.body<T>()
    }
