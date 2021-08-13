package models

import kotlinx.serialization.Serializable

@Serializable
data class Issue(val title: String, val html_url: String, val body: String, val url: String)