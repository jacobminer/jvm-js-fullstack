package models

import kotlinx.serialization.Serializable

@Serializable
data class TimelineItem(val actor: Actor?, val source: Source?, val created_at: String, val changeType: String?)