package models

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val body: String? = null,
    val user: Actor? = null,
    val url: String,
    val updated_at: String? = null,
    val created_at: String
)