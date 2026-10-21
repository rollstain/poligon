package ru.urent.qatools.base_bike_setup.domain

import ru.urent.qatools.base_bike_setup.entities.BikeContext

private const val BIKE_QR_CODE_URL_PREFIX = "http://ure.su/a/"

fun bikeQrCodeText(bike: BikeContext): String = "$BIKE_QR_CODE_URL_PREFIX${bike.identifier}"
