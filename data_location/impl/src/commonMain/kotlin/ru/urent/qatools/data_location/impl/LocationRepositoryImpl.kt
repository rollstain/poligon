package ru.urent.qatools.data_location.impl

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.core_network.StandHttpClientProvider
import ru.urent.qatools.core_network.requestResultOf
import ru.urent.qatools.data_location.LocationRepository
import ru.urent.qatools.data_location.ZoneType
import ru.urent.qatools.data_location.request.ParkingListRequest
import ru.urent.qatools.data_location.response.CitiesResponse
import ru.urent.qatools.data_location.response.ParkingListResponse
import ru.urent.qatools.data_location.response.TechZonesResponse

internal class LocationRepositoryImpl(private val clientProvider: StandHttpClientProvider) : LocationRepository {

    override suspend fun getCities(): RequestResult<CitiesResponse> = requestResultOf {
        clientProvider.httpClient.get("location/api/areas/city").body()
    }

    override suspend fun getTechZones(cityIds: List<String>): RequestResult<TechZonesResponse> = requestResultOf {
        clientProvider.httpClient.post("gateway-ocelot/api/v1/location-api/Zones/techs/byAreaId") {
            contentType(ContentType.Application.Json)
            setBody(cityIds)
        }.body()
    }

    override suspend fun getCityZones(cityId: String): RequestResult<List<ParkingListResponse.Entry?>> = requestResultOf {
        val request = ParkingListRequest(areaIds = listOf(cityId), types = ZoneType.entries, withDisabled = true)
        val entries = mutableListOf<ParkingListResponse.Entry?>()
        var page = FIRST_PAGE
        do {
            val response: ParkingListResponse = clientProvider.httpClient.post("gateway-ocelot/api/v1/location-api/sapp/Parking/list") {
                parameter(PAGE_PARAMETER, page)
                parameter(PAGE_SIZE_PARAMETER, ZONES_PAGE_SIZE)
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            entries.addAll(response.entries.orEmpty())
            val totalPages = response.totalPages
            page += 1
        } while (totalPages != null && page <= totalPages)
        entries
    }

    private companion object {
        const val PAGE_PARAMETER = "cPage"
        const val PAGE_SIZE_PARAMETER = "iOnPage"
        const val FIRST_PAGE = 1
        const val ZONES_PAGE_SIZE = 1000
    }
}
