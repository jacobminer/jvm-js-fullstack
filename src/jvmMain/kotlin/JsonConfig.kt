import kotlinx.serialization.json.Json

object JsonConfig {
    val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
}