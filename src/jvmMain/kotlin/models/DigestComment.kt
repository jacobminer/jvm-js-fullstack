package models

import java.time.Instant

data class DigestComment(val body: String, val date: Instant, val url: String)