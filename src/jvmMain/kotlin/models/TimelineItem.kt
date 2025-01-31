package models

import kotlinx.serialization.Serializable

@Serializable
data class TimelineItem(
    val actor: Actor? = null,
    val source: Source? = null,
    val created_at: String,
    var changeType: String? = null,
    val event: String? = null,
    val url: String? = null,
    val milestone: Milestone? = null,
    val label: Label? = null
)