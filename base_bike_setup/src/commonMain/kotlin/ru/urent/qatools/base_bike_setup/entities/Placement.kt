package ru.urent.qatools.base_bike_setup.entities

data class Placement(
    val parkingName: String?,
    val isInsideRentZone: Boolean,
    val isInsideNoRidingZone: Boolean,
    val isInsideNoParkingZone: Boolean,
)
