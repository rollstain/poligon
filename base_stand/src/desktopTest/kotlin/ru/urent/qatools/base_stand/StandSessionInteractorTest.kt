package ru.urent.qatools.base_stand

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import ru.urent.qatools.core_database.CoreDatabase
import ru.urent.qatools.core_logger.AppLog
import ru.urent.qatools.core_network.HttpClientFactory
import ru.urent.qatools.core_network.impl.HttpClientFactoryImpl
import ru.urent.qatools.core_platform.TokenCipher
import ru.urent.qatools.data_stand.impl.DataStandModule
import ru.urent.qatools.testing_stand.RecordingLog
import ru.urent.qatools.testing_stand.TestEngineFactory

class StandSessionInteractorTest {
    private val log = RecordingLog()

    @Test
    fun standThatForbidsItsPublicConfigAsksForVpn() = runTest {
        val interactor = createInteractor()

        val result = interactor.login(rawBaseUrl = CLOSED_STAND, email = EMAIL, password = PASSWORD, rememberPassword = true)

        assertEquals(LoginResult.VpnRequired, result)
    }

    @Test
    fun loginAfterVpnIsTurnedOnUsesNewConnection() = runTest {
        val interactor = createInteractor(clientsOpenedWithoutVpn = 1)
        val withoutVpn = interactor.login(rawBaseUrl = FIRST_STAND, email = EMAIL, password = PASSWORD, rememberPassword = true)

        val withVpn = interactor.login(rawBaseUrl = FIRST_STAND, email = EMAIL, password = PASSWORD, rememberPassword = true)

        assertEquals(LoginResult.VpnRequired, withoutVpn)
        assertEquals(LoginResult.Success, withVpn)
        assertEquals(FIRST_STAND, interactor.session.value?.baseUrl)
    }

    @Test
    fun rememberedPasswordIsOfferedToLoginForm() = runTest {
        val interactor = createInteractor()

        interactor.login(rawBaseUrl = FIRST_STAND, email = EMAIL, password = PASSWORD, rememberPassword = true)

        assertEquals(PASSWORD, interactor.getSavedPassword(FIRST_STAND))
    }

    @Test
    fun passwordIsNotOfferedWhenNotRemembered() = runTest {
        val interactor = createInteractor()

        interactor.login(rawBaseUrl = FIRST_STAND, email = EMAIL, password = PASSWORD, rememberPassword = false)

        assertNull(interactor.getSavedPassword(FIRST_STAND))
    }

    @Test
    fun logoutStopsOfferingPassword() = runTest {
        val interactor = createInteractor()
        interactor.login(rawBaseUrl = FIRST_STAND, email = EMAIL, password = PASSWORD, rememberPassword = true)

        interactor.logout()

        assertNull(interactor.getSavedPassword(FIRST_STAND))
    }

    @Test
    fun logoutForgetsSavedLoginOnEveryStand() = runTest {
        val interactor = createInteractor()
        interactor.login(rawBaseUrl = FIRST_STAND, email = EMAIL, password = PASSWORD, rememberPassword = true)
        interactor.switchTo(SECOND_STAND)

        interactor.logout()

        assertEquals(listOf(false, false), interactor.refreshSavedStands().map { it.hasSavedLogin })
    }

    @Test
    fun forgettingCurrentStandEndsSessionAndRemovesItFromSavedStands() = runTest {
        val interactor = createInteractor()
        interactor.login(rawBaseUrl = FIRST_STAND, email = EMAIL, password = PASSWORD, rememberPassword = true)
        interactor.switchTo(SECOND_STAND)

        interactor.forget(SECOND_STAND)

        assertNull(interactor.session.value)
        assertEquals(listOf(FIRST_STAND), interactor.savedStands.value.map { it.baseUrl })
    }

    @Test
    fun loginLeavesPasswordOutOfTheLog() = runTest {
        val interactor = createInteractor()

        interactor.login(rawBaseUrl = FIRST_STAND, email = EMAIL, password = PASSWORD, rememberPassword = true)

        assertTrue(log.lines.any { it.contains(TOKEN_PATH) })
        assertTrue(log.lines.none { it.contains(PASSWORD) })
    }

    private fun createInteractor(clientsOpenedWithoutVpn: Int = 0): StandSessionInteractor {
        var openedClients = 0
        val engineFactory = TestEngineFactory {
            val isOpenedWithoutVpn = openedClients < clientsOpenedWithoutVpn
            openedClients += 1
            standEngine(isOpenedWithoutVpn)
        }
        val koin = koinApplication {
            modules(
                DataStandModule().module,
                BaseStandModule().module,
                module {
                    single { CoreDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, schema = CoreDatabase.Schema)) }
                    single<HttpClientFactory> { HttpClientFactoryImpl(engineFactory = engineFactory, cachingEngineFactory = engineFactory, log = log) }
                    single<TokenCipher> { PlainTokenCipher }
                    single<AppLog> { log }
                },
            )
        }.koin
        return koin.get()
    }

    private fun standEngine(isOpenedWithoutVpn: Boolean) = MockEngine { request ->
        val path = request.url.encodedPath
        val isClosedToClient = isOpenedWithoutVpn || request.url.host == CLOSED_HOST
        when {
            path == CONFIG_PATH && isClosedToClient -> respond("", HttpStatusCode.Forbidden)
            path == CONFIG_PATH -> respond(CONFIG_JSON, HttpStatusCode.OK, JsonHeaders)
            path == TOKEN_PATH -> respond(TOKEN_JSON, HttpStatusCode.OK, JsonHeaders)
            else -> respond("", HttpStatusCode.NotFound)
        }
    }

    private object PlainTokenCipher : TokenCipher {
        override fun encrypt(plain: ByteArray): ByteArray = plain

        override fun decrypt(encrypted: ByteArray): ByteArray = encrypted
    }

    private companion object {
        const val CLOSED_HOST = "closed.test"
        const val CLOSED_STAND = "https://$CLOSED_HOST"
        const val FIRST_STAND = "https://first.test"
        const val SECOND_STAND = "https://second.test"
        const val EMAIL = "qa@urent.test"
        const val PASSWORD = "password"
        const val CONFIG_PATH = "/admin/assets/config.json"
        const val TOKEN_PATH = "/identity/connect/token"
        const val CONFIG_JSON = """{"auth":{"clientId":"admin.client","clientSecret":"secret","scope":"api"}}"""
        const val TOKEN_JSON = """{"access_token":"token"}"""
        val JsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
