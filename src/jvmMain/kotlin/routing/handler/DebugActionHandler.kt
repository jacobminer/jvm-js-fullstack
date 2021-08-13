package routing.handler

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import routing.DebugRequest
import services.digest.DigestService

class DebugActionHandler {
    suspend fun handleDebugAction(call: ApplicationCall) {
        val payload = call.receive<DebugRequest>()

        if (payload.url == null) {
            val message = "Ignoring because action did not include url, please post issue number in url"
            call.respondText { message }
            return
        }

        val url = payload.url
        if (url.contains("issues")) {
            println("DebugRouter: Handling issue action")
            DigestService.debugDigestIssue(url)
        } else if (url.contains("repos")) {
            println("DebugRouter: Handling repo action")
            DigestService.debugDigestRepo(url)
        } else if (url.contains("users")) {
            println("DebugRouter: Handling user action")
            DigestService.debugDigestAllRepos(url)
        }
        call.respondText { "yay!" }
    }
}