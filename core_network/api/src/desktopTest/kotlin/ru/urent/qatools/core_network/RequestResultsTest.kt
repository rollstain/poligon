package ru.urent.qatools.core_network

import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import ru.urent.qatools.core_entities.RequestFailure
import ru.urent.qatools.core_entities.RequestResult

class RequestResultsTest {

    @Test
    fun clientErrorsBecomeTypedFailures() = runTest {
        assertEquals(RequestResult.Error(RequestFailure.NoConnection), requestResultOf<Unit> { throw IOException(BROKEN_CONNECTION) })
        assertEquals(
            RequestResult.Error(RequestFailure.NoConnection),
            requestResultOf<Unit> { throw StandUnavailableException(IOException(BROKEN_CONNECTION)) },
        )
        assertEquals(RequestResult.Error(RequestFailure.SessionExpired), requestResultOf<Unit> { throw SessionExpiredException() })
        assertEquals(
            RequestResult.Error(RequestFailure.Http(FORBIDDEN_STATUS_CODE)),
            requestResultOf<Unit> { throw UnexpectedResponseException(FORBIDDEN_STATUS_CODE, PATH) },
        )
        assertEquals(RequestResult.Error(RequestFailure.InvalidResponse), requestResultOf<Unit> { throw SerializationException() })
    }

    @Test
    fun contentErrorOfCancelledLoadIsTreatedAsCancellation() = runTest {
        val load = async(SupervisorJob()) {
            requestResultOf<Unit> {
                currentCoroutineContext().cancel()
                throw IllegalStateException(CONTENT_LENGTH_MISMATCH)
            }
        }

        assertFailsWith<CancellationException> { load.await() }
    }

    @Test
    fun stateErrorOfActiveLoadIsNotHidden() = runTest {
        assertFailsWith<IllegalStateException> {
            requestResultOf<Unit> { throw IllegalStateException(CONTENT_LENGTH_MISMATCH) }
        }
    }

    private companion object {
        const val BROKEN_CONNECTION = "Соединение разорвано"
        const val FORBIDDEN_STATUS_CODE = 403
        const val PATH = "/bikes/api/ping"
        const val CONTENT_LENGTH_MISMATCH = "Content-Length mismatch: expected 97000 bytes, but received 71236 bytes"
    }
}
