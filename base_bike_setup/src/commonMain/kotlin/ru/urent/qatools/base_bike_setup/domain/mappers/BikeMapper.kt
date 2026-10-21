package ru.urent.qatools.base_bike_setup.domain.mappers

import ru.urent.qatools.base_bike_setup.entities.DisabledReason
import ru.urent.qatools.data_bikes.response.DisabledReasonsResponse

class BikeMapper {

    fun toDisabledReasons(response: DisabledReasonsResponse): List<DisabledReason> {
        val reasons = mutableListOf<DisabledReason>()
        for (entry in response.entries.orEmpty()) {
            val id = entry?.id
            val name = entry?.name
            val title = entry?.description
            if (id == null || name == null || title == null || entry.isHidden == true) continue
            reasons.add(DisabledReason(id = id, name = name, title = title))
        }
        return reasons
    }
}
