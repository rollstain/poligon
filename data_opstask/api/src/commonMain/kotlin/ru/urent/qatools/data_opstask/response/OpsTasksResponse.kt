package ru.urent.qatools.data_opstask.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.urent.qatools.core_entities.TaskPriority

@Serializable
data class OpsTasksResponse(
    @SerialName("entries")
    val entries: List<Entry>?,
) {
    @Serializable
    data class Entry(
        @SerialName("priority")
        val priority: TaskPriority?,
    )
}
