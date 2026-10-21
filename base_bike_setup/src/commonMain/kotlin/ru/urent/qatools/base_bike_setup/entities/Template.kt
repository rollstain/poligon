package ru.urent.qatools.base_bike_setup.entities

import kotlin.time.Instant

data class Template(val id: Long, val name: String, val bikes: List<TemplateBike>)

data class TemplateBike(val id: Long, val setup: BikeSetup)

data class SetupDefaults(val target: ChargeTarget, val availability: AvailabilityTarget)

data class BikeApplyResult(val appliedSetup: BikeSetup, val status: ApplyStatus, val appliedAt: Instant)

data class SavedCity(val id: String, val name: String)
