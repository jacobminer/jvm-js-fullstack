import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import models.WebHookContent

fun Route.webhookRoute() {
    val labelName = "xp-"
    val labelActionHandler = LabelActionHandler(labelName)
    fun verifyGitHub(req: ApplicationRequest): Boolean {
        if (req.header("user-agent")?.contains("GitHub-Hookshot") == false) {
            println("WebHookRouter: User agent isn't GithHub-Hookshot")
            return false
        }

        // TODO: 2021-08-13 add crypto info
//        val theirSignature = req.header("x-hub-signature")
////        log.debug("WebHookRouter", "verify: theirSignature = " + theirSignature)
//        val payload = JSON.stringify(req.body)
//        val secret = System.getenv("webhookKey")
//        val ourSignature = "sha1=${crypto.createHmac("sha1", secret).update(payload, "utf8").digest("hex")}"
////        log.debug("WebHookRouter", "verify: ourSignature = " + ourSignature)
//        return crypto.timingSafeEqual(Buffer.from(theirSignature), Buffer.from(ourSignature))
        return true
    }

    post("/webhook") {
        if (!verifyGitHub(call.request)) {
            call.respondText { "Ignoring because GitHub auth failed" }
            return@post
        }

        val content = call.receive<WebHookContent>()

        if (content.action != LabelActionHandler.labelAction) {
            val message = "Ignoring because action was ${content.action}, which is not supported."
            println("WebHookRouter: $message")
            call.respondText { message }
            return@post
        }

        println("WebHookRouter: Handling labeled action")
        labelActionHandler.handleLabeledAction(content, call)
    }
}