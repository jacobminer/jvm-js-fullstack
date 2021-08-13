import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

object GithubClient {
    private val client = HttpClient(CIO)

    suspend fun postToUrl(json: Any, url: String): HttpResponse {
        println("postToUrl: Calling POST to $url");
        return client.post(url) {
            body = json
            headers {
                append("Content-Type", "application/json")
                append("User-Agent", "steamclockkmp-bot")
                append("Authorization", "token ${System.getenv("crossplat")}")
            }
        }
    }
}