package client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import logging.log

object GithubClient {
    private lateinit var _client: HttpClient
    fun getClient(): HttpClient {
        if (!this::_client.isInitialized) {
            _client = HttpClient(CIO) {
                install(JsonFeature) { serializer = KotlinxSerializer() }
            }
        }
        return _client
    }

    fun getUserAgent() = "steamclockkmp-bot"
    fun getToken() = "token ${System.getenv("crossplat")}"

    suspend fun <T: Any> postToUrl(url: String, content: T): HttpResponse {
        println("postToUrl: Calling POST to $url");
        return getClient().post(url) {
            contentType(ContentType.Application.Json)
            body = content
            headers {
                append("User-Agent", getUserAgent())
                append("Authorization", getToken())
            }
        }
    }

    suspend inline fun <reified T: Any> getFromUrl(issueUrl: String): T {
        log.debug("getFromUrl", "Calling GET for url = $issueUrl");
        return getClient().get(issueUrl) {
            contentType(ContentType.Application.Json)
            headers {
                append("User-Agent", getUserAgent())
                append("Authorization", getToken())
            }
        }
    }
}