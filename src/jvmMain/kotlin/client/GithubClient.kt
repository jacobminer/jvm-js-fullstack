package client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

object GithubClient {
    private val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

    suspend fun <T: Any> postToUrl(url: String, content: T): HttpResponse {
        println("postToUrl: Calling POST to $url");
        return client.post(url) {
            contentType(ContentType.Application.Json)
            body = content
            headers {
                append("User-Agent", "steamclockkmp-bot")
                append("Authorization", "token ${System.getenv("crossplat")}")
            }
        }
    }
}