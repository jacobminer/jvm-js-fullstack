package models

import kotlinx.serialization.Serializable

@Serializable
data class NewComment(val body: String)