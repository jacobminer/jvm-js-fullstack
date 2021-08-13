import io.ktor.application.*
import io.ktor.features.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import routing.debugRoute
import routing.webhookRoute

fun main() {
    embeddedServer(Netty, System.getenv("PORT").toInt()) {
        install(ContentNegotiation) {
            json(JsonConfig.json)
        }
        routing {
            webhookRoute()
            debugRoute()
        }
    }.start(wait = true)
}