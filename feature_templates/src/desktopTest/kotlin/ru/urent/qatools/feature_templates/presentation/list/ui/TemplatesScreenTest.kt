package ru.urent.qatools.feature_templates.presentation.list.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.BikeSetup
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.core_entities.ChargeState
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.feature_templates.presentation.list.mvi.TemplatesScreenModel
import ru.urent.qatools.navigation.LocalNavigator
import ru.urent.qatools.navigation.Screens
import ru.urent.qatools.testing_stand.RecordingNavigator
import ru.urent.qatools.testing_stand.SilentLog
import ru.urent.qatools.testing_stand.TestStandInteractors
import ru.urent.qatools.uikit.theme.QaTheme

@OptIn(ExperimentalTestApi::class)
class TemplatesScreenTest {
    private val navigator = RecordingNavigator()
    private val interactors = TestStandInteractors {
        MockEngine { request ->
            when (request.url.encodedPath) {
                "/location/api/areas/city" -> respond(CITIES_JSON, HttpStatusCode.OK, JsonHeaders)
                "/gateway-ocelot/api/v1/location-api/Zones/techs/byAreaId" -> respond(TECH_ZONES_JSON, HttpStatusCode.OK, JsonHeaders)
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
    }

    @Test
    fun rowShowsBikesTargetsAccessAndPlace() = runDesktopComposeUiTest(width = CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        createTemplate()
        showTemplates(this)

        waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) { onAllNodesWithText(TECH_ZONE_NAME).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("S.4071").assertIsDisplayed()
        onNodeWithText("Низкий заряд ×2, Рекомендуемая замена ×1").assertIsDisplayed()
        onNodeWithText("снять с доступа: 1").assertIsDisplayed()
        onNodeWithText(CITY_NAME).assertIsDisplayed()
        onNodeWithText("Ещё не применялся").assertIsDisplayed()
    }

    @Test
    fun clickingRowOpensTemplate() = runDesktopComposeUiTest(width = CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        val templateId = createTemplate()
        showTemplates(this)

        onNodeWithText(TEMPLATE_NAME).performClick()

        waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) { navigator.pushedScreens.isNotEmpty() }
        assertEquals(listOf<Screens>(Screens.Template(standBaseUrl = TestStandInteractors.BASE_URL, templateId = templateId)), navigator.pushedScreens)
    }

    private fun createTemplate(): Long = runBlocking {
        val templateInteractor = interactors.templateInteractor
        val templateId = templateInteractor.create(standBaseUrl = TestStandInteractors.BASE_URL, name = TEMPLATE_NAME)
        val low = ChargeTarget.State(ChargeState.LOW)
        templateInteractor.addBike(templateId, BikeSetup(4071, InsideTechZone, low, AvailabilityTarget.Keep))
        templateInteractor.addBike(templateId, BikeSetup(4072, InsideTechZone, low, AvailabilityTarget.Disabled(REASON_NAME, REASON_TITLE)))
        templateInteractor.addBike(templateId, BikeSetup(4075, InsideTechZone, ChargeTarget.State(ChargeState.NEED_TO_CHANGE), AvailabilityTarget.Keep))
        templateId
    }

    private fun showTemplates(test: DesktopComposeUiTest) {
        val screenModel = TemplatesScreenModel(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            standBaseUrl = TestStandInteractors.BASE_URL,
            templateInteractor = interactors.templateInteractor,
            locationInteractor = interactors.locationInteractor,
            runner = interactors.runner,
            log = SilentLog,
        )
        val screen = TemplatesScreen(arguments = Screens.Templates(standBaseUrl = TestStandInteractors.BASE_URL), screenModel = screenModel)
        test.setContent {
            CompositionLocalProvider(LocalNavigator provides navigator) {
                QaTheme(isDark = false) { screen.Content() }
            }
        }
        test.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) { screenModel.stateFlow.value.templates.isNotEmpty() }
        test.waitForIdle()
    }

    private companion object {
        const val CONTENT_WIDTH = 1296
        const val CONTENT_HEIGHT = 868
        const val WAIT_TIMEOUT_MS = 5_000L
        const val TEMPLATE_NAME = "Низкий заряд, Адлер"
        const val CITY_NAME = "Адлер"
        const val TECH_ZONE_NAME = "Адлер 4.1"
        const val REASON_NAME = "Repair"
        const val REASON_TITLE = "Требует ремонта"
        const val ADLER_ID = "5f9b2468f0e5ce0001b638e9"
        val InsideTechZone = GeoPoint(latitude = 43.43357, longitude = 39.92593)
        val JsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        const val CITIES_JSON = """
            {
              "entries": [
                {"id": "$ADLER_ID", "name": "$CITY_NAME", "isCity": true, "isDisable": false,
                 "center": {"lat": 43.425258324884446, "lng": 39.92378847312527}}
              ]
            }
        """
        const val TECH_ZONES_JSON = """
            {
              "entries": [
                {"id": "z1", "name": "$TECH_ZONE_NAME", "areaId": "$ADLER_ID", "disabled": false, "type": "TECH",
                 "center": {"lat": 43.4336, "lng": 39.926},
                 "coordinates": [{"lat": 43.4338, "lng": 39.9252}, {"lat": 43.4338, "lng": 39.9264}, {"lat": 43.4331, "lng": 39.9262}]}
              ],
              "errors": [],
              "succeeded": true
            }
        """
    }
}
