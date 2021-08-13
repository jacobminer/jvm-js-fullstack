package models

import kotlinx.serialization.Serializable
import models.Issue

@Serializable
data class Source(val type: String, val issue: Issue)