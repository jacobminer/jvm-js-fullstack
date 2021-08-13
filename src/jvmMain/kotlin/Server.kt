import io.ktor.application.*
import io.ktor.features.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import routing.debugRoute
import routing.webhookRoute
import services.digest.DigestService

fun main() {
    GlobalScope.launch(Dispatchers.IO) {
        DigestService.beginDigest()
    }
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