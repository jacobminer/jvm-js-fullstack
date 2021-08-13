package models

import kotlinx.serialization.Serializable

@Serializable
data class Comment(val body: String, val user: Actor, val url: String, val updated_at: String?, val created_at: String)