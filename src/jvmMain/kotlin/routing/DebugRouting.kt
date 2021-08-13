package routing

import io.ktor.application.*
import io.ktor.routing.*
import kotlinx.serialization.Serializable
import routing.handler.DebugActionHandler

@Serializable
data class DebugRequest(val url: String?)

fun Route.debugRoute() {
    val debugActionHandler = DebugActionHandler()

    post("/debug") {
        debugActionHandler.handleDebugAction(call)
    }
}