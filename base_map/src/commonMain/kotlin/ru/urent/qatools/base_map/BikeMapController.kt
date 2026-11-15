package ru.urent.qatools.base_map

import ru.urent.qatools.core_entities.GeoPoint

interface BikeMapController {
    fun focus(point: GeoPoint)
}
