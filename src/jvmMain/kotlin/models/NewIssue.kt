package models

import kotlinx.serialization.Serializable

@Serializable
data class NewIssue(val title: String?, val body: String)