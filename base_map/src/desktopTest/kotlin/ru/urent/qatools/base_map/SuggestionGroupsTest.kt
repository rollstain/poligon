package ru.urent.qatools.base_map

import kotlin.test.Test
import kotlin.test.assertEquals
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.ChargeThresholds
import ru.urent.qatools.base_bike_setup.entities.City
import ru.urent.qatools.base_bike_setup.entities.DisabledReason
import ru.urent.qatools.base_bike_setup.entities.TechZone
import ru.urent.qatools.base_bike_setup.presentation.availabilitySuggestionGroups
import ru.urent.qatools.base_bike_setup.presentation.chargeSuggestionGroups
import ru.urent.qatools.core_entities.GeoPoint

class SuggestionGroupsTest {

    @Test
    fun reasonsThatCreateDriverTaskComeBeforeOtherReasons() {
        val groups = availabilitySuggestionGroups(query = "", reasons = Reasons)

        assertEquals(listOf(null, DRIVER_TASK_GROUP, OTHER_REASONS_GROUP), groups.map { it.title })
        assertEquals(listOf(AvailabilityTarget.Keep, AvailabilityTarget.Available), groups[0].suggestions.map { it.value })
        assertEquals(listOf(READY_FOR_USE_TITLE, NEEDS_REPAIR_TITLE), groups[1].suggestions.map { it.title })
        assertEquals(listOf(AUTO_REPAIR_TITLE, REPAIR_TITLE, WAITING_FOR_SERVICE_TITLE), groups[2].suggestions.map { it.title })
    }

    @Test
    fun queryKeepsOnlyMatchingReasonsInStandOrder() {
        val groups = availabilitySuggestionGroups(query = "рем", reasons = Reasons)

        assertEquals(listOf(DRIVER_TASK_GROUP, OTHER_REASONS_GROUP), groups.map { it.title })
        assertEquals(listOf(NEEDS_REPAIR_TITLE), groups[0].suggestions.map { it.title })
        assertEquals(listOf(AUTO_REPAIR_TITLE, REPAIR_TITLE, WAITING_FOR_SERVICE_TITLE), groups[1].suggestions.map { it.title })
    }

    @Test
    fun numberInChargeQueryBecomesExactPercent() {
        val suggestions = chargeSuggestionGroups(query = "37,5", thresholds = Thresholds).single().suggestions

        assertEquals(listOf<ChargeTarget>(ChargeTarget.Percent(EXACT_PERCENT)), suggestions.map { it.value })
    }

    @Test
    fun chargeStatesShowRangesOfBikeModel() {
        val suggestions = chargeSuggestionGroups(query = "низк", thresholds = Thresholds).single().suggestions

        assertEquals(
            listOf("Сверхнизкий заряд" to "0–10%", "Низкий заряд" to "10–15%"),
            suggestions.map { it.title to it.supportingText },
        )
    }

    @Test
    fun coordinatesInPlaceQueryBecomeOnePoint() {
        val groups = placeSuggestionGroups(query = "43.42482, 39.92811", cityName = CITY_NAME, cities = Cities, techZones = TechZones)

        assertEquals(listOf<MapPlace>(MapPlace.PointPlace(GeoPoint(43.42482, 39.92811))), groups.single().suggestions.map { it.value })
    }

    @Test
    fun longTechZoneListIsCutAndHiddenCountIsShown() {
        val groups = placeSuggestionGroups(query = CITY_NAME, cityName = CITY_NAME, cities = Cities, techZones = TechZones)

        assertEquals(MAX_SHOWN_PER_GROUP, groups[0].suggestions.size)
        assertEquals(TECH_ZONE_COUNT - MAX_SHOWN_PER_GROUP, groups[0].hiddenCount)
        assertEquals(listOf(CITY_NAME), groups[1].suggestions.map { it.title })
    }

    private companion object {
        const val DRIVER_TASK_GROUP = "Снять — стенд создаст задачу водителю"
        const val OTHER_REASONS_GROUP = "Снять — другие причины"
        const val AUTO_REPAIR_TITLE = "Авторемонт"
        const val READY_FOR_USE_TITLE = "Готов к эксплуатации"
        const val REPAIR_TITLE = "Ремонт"
        const val NEEDS_REPAIR_TITLE = "Требует ремонта"
        const val WAITING_FOR_SERVICE_TITLE = "Ожидание ремонта СЦ"
        const val EXACT_PERCENT = 37.5
        const val CITY_NAME = "Адлер"
        const val TECH_ZONE_COUNT = 10
        const val MAX_SHOWN_PER_GROUP = 8
        val Center = GeoPoint(43.42, 39.92)
        val Thresholds = ChargeThresholds(emptyPercent = 0.0, powerOffPercent = 10.0, disablePercent = 15.0, needToChangePercent = 25.0)
        val Reasons = listOf(
            DisabledReason(id = "1", name = "AutoRepair", title = AUTO_REPAIR_TITLE),
            DisabledReason(id = "2", name = "ReadyForUse", title = READY_FOR_USE_TITLE),
            DisabledReason(id = "3", name = "Repair", title = REPAIR_TITLE),
            DisabledReason(id = "4", name = "NeedsRepair", title = NEEDS_REPAIR_TITLE),
            DisabledReason(id = "5", name = "WaitingForServiceCenter", title = WAITING_FOR_SERVICE_TITLE),
        )
        val Cities = listOf(City(id = "adler", name = CITY_NAME, center = Center), City(id = "sochi", name = "Сочи", center = Center))
        val TechZones = (1..TECH_ZONE_COUNT).map { number ->
            TechZone(id = number.toString(), name = "$CITY_NAME $number", center = Center, outline = emptyList())
        }
    }
}
