package models

import kotlinx.serialization.Serializable

@Serializable
data class WebHookContent(val action: String, val issue: Issue? = null, val label: Label?)