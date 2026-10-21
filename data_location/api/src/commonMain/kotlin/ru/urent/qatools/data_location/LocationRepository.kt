package ru.urent.qatools.data_location

import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.data_location.response.CitiesResponse
import ru.urent.qatools.data_location.response.ParkingListResponse
import ru.urent.qatools.data_location.response.TechZonesResponse

interface LocationRepository {
    suspend fun getCities(): RequestResult<CitiesResponse>
    suspend fun getTechZones(cityIds: List<String>): RequestResult<TechZonesResponse>
    suspend fun getCityZones(cityId: String): RequestResult<List<ParkingListResponse.Entry?>>
}
