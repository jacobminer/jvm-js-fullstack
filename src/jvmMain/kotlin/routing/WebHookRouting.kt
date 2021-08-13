package routing

import io.ktor.application.*
import io.ktor.routing.*
import routing.handler.WebhookHandler

fun Route.webhookRoute() {
    val webHookHandler = WebhookHandler()

    post("/webhook") {
        webHookHandler.handleLabeledAction(call)
    }
}