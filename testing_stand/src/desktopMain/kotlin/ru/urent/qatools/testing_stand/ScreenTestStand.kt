package ru.urent.qatools.testing_stand

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import ru.urent.qatools.base_map.MapTileLoader
import ru.urent.qatools.core_entities.TileSource

class ScreenTestStand(private val rejectedBikeRequests: Int = 0) {
    private var bikeRequests = 0
    private val handler: MockRequestHandler = { request ->
        when (request.url.encodedPath) {
            SIMULATOR_STATUS_PATH -> respond(SIMULATOR_STATUS_JSON, HttpStatusCode.OK, TextHeaders)
            "/trackers/api/trackers/tracker/$IMEI" -> respond(TRACKER_JSON, HttpStatusCode.OK, JsonHeaders)
            "/bikes/api/bikes/identifier/S.$FOUND_BIKE_NUMBER" -> {
                val isRejected = bikeRequests < rejectedBikeRequests
                bikeRequests += 1
                if (isRejected) respondError(HttpStatusCode.Forbidden) else respond(BIKE_JSON, HttpStatusCode.OK, JsonHeaders)
            }
            "/drivernavtelecom/api/trackers/tracker/$IMEI" -> respond(fixture("/tracker-driver.json"), HttpStatusCode.OK, JsonHeaders)
            "/gateway-adm/api/bikes/models/$MODEL_ID" -> respond(fixture("/model.json"), HttpStatusCode.OK, JsonHeaders)
            "/gateway-ocelot/api/v1/pickupdropoff-api/tasks/byTransportId/S.$FOUND_BIKE_NUMBER" ->
                respond(fixture("/driver-task.json"), HttpStatusCode.OK, JsonHeaders)
            "/bikes/api/disabledreasons/allowed" -> respond(fixture("/disabled-reasons.json"), HttpStatusCode.OK, JsonHeaders)
            "/gateway-ocelot/api/v1/location-api/sapp/Parking/list" -> respond(PARKING_LIST_JSON, HttpStatusCode.OK, JsonHeaders)
            else -> respondError(HttpStatusCode.NotFound)
        }
    }
    val interactors = TestStandInteractors { MockEngine(handler) }
    val tileLoader = MapTileLoader(httpClient = HttpClient(MockEngine(handler)), log = SilentLog)

    private fun fixture(path: String): String = checkNotNull(javaClass.getResource(path)) { "Нет фикстуры $path" }.readText()

    companion object {
        const val BASE_URL = TestStandInteractors.BASE_URL
        const val FOUND_BIKE_NUMBER = 4071
        const val MISSING_BIKE_NUMBER = 4072
        const val BIKE_PARKING_NAME = "Тестовая парковка"
        val Tiles = TileSource(urlTemplate = "https://tiles.test/{z}/{x}/{y}", subdomains = emptyList(), urlSuffix = "")

        private const val IMEI = "300000000004071"
        private const val MODEL_ID = "6065d2f5ef64db1143f1dec4"
        private const val SIMULATOR_STATUS_PATH = "/simulator-signal/api/status"
        private const val SIMULATOR_STATUS_JSON =
            """{"ID":4071,"Lat":"43.42485","Lon":"39.92816","I1":false,"Startid":4000,"Countid":110,"Ping":60}"""
        private const val TRACKER_JSON = """
            {
              "identifier": "S.4071",
              "driver": "driver.scooter.tracker.navtelecom.api",
              "location": {"lat": 43.424838, "lng": 39.928128},
              "telemetry": {"telemetry": {"mainBatteryVoltage": 38.46}}
            }
        """
        private const val BIKE_JSON = """
            {
              "id": "605b34168c1efdb51cfed7a4",
              "identifier": "S.4071",
              "vendorId": "URB",
              "trackerId": "$IMEI",
              "model": {"id": "6065d2f5ef64db1143f1dec4", "name": "Ninebot L 60L"},
              "bikeCity": {"cityId": "5f9b2468f0e5ce0001b638e9", "cityName": "Адлер"},
              "charge": {"batteryPercent": 0.625, "status": "Ok"},
              "state": {
                "state": {"connected": true, "connectionLostDate": null, "nullableCoordinates": false, "nullableCoordinatesDateTime": null, "disabled": true},
                "mainDisabledReason": "Готов к эксплуатации",
                "disabledReasonTags": ["ReadyForUse"]
              },
              "bikeBatteryInternal": null
            }
        """
        private const val PARKING_LIST_JSON = """
            {
              "entries": [
                {
                  "id": "bike-parking",
                  "name": "$BIKE_PARKING_NAME",
                  "isDisabled": false,
                  "center": {"lat": 43.424838, "lng": 39.928128},
                  "coordinates": [],
                  "type": "CIRCLEPARKING",
                  "radius": 30.0
                }
              ],
              "totalPages": 1
            }
        """
        private val JsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        private val TextHeaders = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
    }
}
