import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object GithubClient {
    private val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

    suspend fun postToUrl(json: String, url: String): HttpResponse {
        println("postToUrl: Calling POST to $url");
        return client.post(url) {
            contentType(ContentType.Application.Json)
            body = json
            headers {
                append("Content-Type", "application/json")
                append("User-Agent", "steamclockkmp-bot")
                append("Authorization", "token ${System.getenv("crossplat")}")
            }
        }
    }
}