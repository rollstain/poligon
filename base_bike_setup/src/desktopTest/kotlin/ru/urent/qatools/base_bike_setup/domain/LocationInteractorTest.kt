package ru.urent.qatools.base_bike_setup.domain

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import ru.urent.qatools.base_bike_setup.entities.City
import ru.urent.qatools.base_bike_setup.entities.MapZones
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.testing_stand.TestStandInteractors

class LocationInteractorTest {
    private var zonePageRequests = 0
    private var techZoneRequests = 0

    @Test
    fun techZonesSkipDisabledOnesAndAreSortedByName() = runTest {
        val interactor = interactor(StandardTestDispatcher(testScheduler))

        val techZones = interactor.getTechZones(ADLER_ID)

        assertEquals(listOf("Адлер 4.1", "АКБ"), techZones.map { it.name })
        assertEquals(GeoPoint(43.4336, 39.926), techZones.last().center)
        assertEquals(3, techZones.last().outline.size)
    }

    @Test
    fun citiesKeepOnlyEnabledCities() = runTest {
        val interactor = interactor(StandardTestDispatcher(testScheduler))

        val cities = interactor.getCities()

        assertEquals(listOf("Адлер"), cities.map { it.name })
        assertEquals(GeoPoint(43.425258324884446, 39.92378847312527), cities.single().center)
    }

    @Test
    fun cityZonesCollectEveryPage() = runTest {
        val interactor = interactor(StandardTestDispatcher(testScheduler))

        val zones = assertIs<RequestResult.Success<MapZones>>(interactor.getCityZones(standBaseUrl = STAND, cityId = ADLER_ID)).data

        assertEquals(listOf("Демократическая"), zones.parkings.map { it.name })
        assertEquals(listOf("rent"), zones.rentZones.map { it.id })
    }

    @Test
    fun cityZonesAreLoadedFromStandOncePerStandAndCity() = runTest {
        val interactor = interactor(StandardTestDispatcher(testScheduler))
        interactor.getCityZones(standBaseUrl = STAND, cityId = ADLER_ID)
        val pageRequestsOfOneLoad = zonePageRequests

        val cached = assertIs<RequestResult.Success<MapZones>>(interactor.getCityZones(standBaseUrl = STAND, cityId = ADLER_ID)).data
        interactor.getCityZones(standBaseUrl = OTHER_STAND, cityId = ADLER_ID)

        assertEquals(listOf("Демократическая"), cached.parkings.map { it.name })
        assertEquals(pageRequestsOfOneLoad * 2, zonePageRequests)
    }

    @Test
    fun techZonesOfAllCitiesComeInOneRequestGroupedByCity() = runTest {
        val interactor = interactor(StandardTestDispatcher(testScheduler))
        val adler = City(id = ADLER_ID, name = "Адлер", center = GeoPoint(43.4253, 39.9238))
        val moscow = City(id = "moscow", name = "Москва", center = GeoPoint(55.7558, 37.6173))

        val grouped = interactor.getTechZonesOfCities(standBaseUrl = STAND, cities = listOf(adler, moscow))
        val cached = interactor.getTechZonesOfCities(standBaseUrl = STAND, cities = listOf(adler, moscow))

        assertEquals(listOf("Адлер"), grouped.map { it.city.name })
        assertEquals(listOf("Адлер 4.1", "АКБ"), grouped.single().techZones.map { it.name })
        assertEquals(grouped, cached)
        assertEquals(1, techZoneRequests)
    }

    @Test
    fun cityInViewIsTheOneWithNearestCenter() = runTest {
        val interactor = interactor(StandardTestDispatcher(testScheduler))
        val moscow = City(id = "moscow", name = "Москва", center = GeoPoint(55.7558, 37.6173))
        val adler = City(id = "adler", name = "Адлер", center = GeoPoint(43.4253, 39.9238))

        val nearAdler = interactor.getNearestCity(cities = listOf(moscow, adler), point = GeoPoint(43.4249, 39.9280))
        val nearMoscow = interactor.getNearestCity(cities = listOf(moscow, adler), point = GeoPoint(55.70, 37.50))

        assertEquals(adler, nearAdler)
        assertEquals(moscow, nearMoscow)
    }

    private fun interactor(dispatcher: CoroutineDispatcher): LocationInteractor {
        val engine = MockEngine(
            MockEngineConfig().apply {
                this.dispatcher = dispatcher
                addHandler { request ->
                    when (request.url.encodedPath) {
                        "/gateway-ocelot/api/v1/location-api/Zones/techs/byAreaId" -> {
                            techZoneRequests += 1
                            respond(TECH_ZONES_JSON, HttpStatusCode.OK, jsonHeaders)
                        }
                        "/location/api/areas/city" -> respond(CITIES_JSON, HttpStatusCode.OK, jsonHeaders)
                        "/gateway-ocelot/api/v1/location-api/sapp/Parking/list" -> {
                            zonePageRequests += 1
                            val pageJson = if (request.url.parameters["cPage"] == "1") ZONES_FIRST_PAGE_JSON else ZONES_SECOND_PAGE_JSON
                            respond(pageJson, HttpStatusCode.OK, jsonHeaders)
                        }
                        else -> respondError(HttpStatusCode.NotFound)
                    }
                }
            },
        )
        return TestStandInteractors { engine }.locationInteractor
    }

    private companion object {
        const val ADLER_ID = "5f9b2468f0e5ce0001b638e9"
        const val STAND = "https://stand.test"
        const val OTHER_STAND = "https://other-stand.test"
        const val TECH_ZONES_JSON = """
            {
              "entries": [
                {"id": "z1", "name": "АКБ", "areaId": "$ADLER_ID", "disabled": false, "type": "TECH",
                 "center": {"lat": 43.4336, "lng": 39.926},
                 "coordinates": [{"lat": 43.4338, "lng": 39.9252}, {"lat": 43.4338, "lng": 39.9264}, {"lat": 43.4331, "lng": 39.9262}]},
                {"id": "z2", "name": "Test Tech Zone", "areaId": "$ADLER_ID", "disabled": true, "type": "TECH",
                 "center": {"lat": 43.41, "lng": 39.91}, "coordinates": []},
                {"id": "z3", "name": "Адлер 4.1", "areaId": "$ADLER_ID", "disabled": false, "type": "TECH",
                 "center": {"lat": 43.42, "lng": 39.93}, "coordinates": [{"lat": 43.421, "lng": 39.931}]}
              ],
              "errors": [],
              "succeeded": true
            }
        """
        const val CITIES_JSON = """
            {
              "entries": [
                {"id": "$ADLER_ID", "name": "Адлер", "isCity": true, "isDisable": false,
                 "center": {"lat": 43.425258324884446, "lng": 39.92378847312527}},
                {"id": "off", "name": "Выключенный", "isCity": true, "isDisable": true, "center": {"lat": 1.0, "lng": 1.0}},
                {"id": "area", "name": "Не город", "isCity": false, "isDisable": false, "center": {"lat": 2.0, "lng": 2.0}}
              ]
            }
        """
        const val ZONES_FIRST_PAGE_JSON = """
            {
              "entries": [
                {"id": "parking", "name": "Демократическая", "isDisabled": false, "type": "CIRCLEPARKING", "radius": 20.0,
                 "center": {"lat": 43.4248, "lng": 39.9281}, "coordinates": []}
              ],
              "totalPages": 2
            }
        """
        const val ZONES_SECOND_PAGE_JSON = """
            {
              "entries": [
                {"id": "rent", "name": "Адлер", "isDisabled": false, "type": "RENT", "center": {"lat": 43.43, "lng": 39.93},
                 "coordinates": [{"lat": 43.40, "lng": 39.90}, {"lat": 43.46, "lng": 39.90}, {"lat": 43.46, "lng": 39.96}]}
              ],
              "totalPages": 2
            }
        """
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
