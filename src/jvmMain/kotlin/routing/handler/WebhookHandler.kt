package routing.handler

import client.GithubClient
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import models.NewIssue
import models.WebHookContent

class WebhookHandler {
    // Make sure you pass in the `url` not the `html_url`!
    private fun getCrossPostingRepoApiUrl(issueUrl: String, repoName: String): String {
        val currentRepoComponents = issueUrl.split("/", ignoreCase = true, limit = 100)
        val otherRepoComponents = currentRepoComponents.slice(0 until currentRepoComponents.count() - 3)
        val otherRepo = otherRepoComponents.joinToString("/") + "/" + repoName
        return otherRepo
    }

    suspend fun handleLabeledAction(call: ApplicationCall) {
        if (!verifyGitHub(call.request)) {
            call.respondText { "Ignoring because GitHub auth failed" }
            return
        }

        val content = call.receive<WebHookContent>()
        if (content.action != WebhookHandler.labelAction) {
            val message = "Ignoring because action was ${content.action}, which is not supported."
            println("WebHookRouter: $message")
            call.respondText { message }
            return
        }

        println("WebHookRouter: Handling labeled action")

        val issue = content.issue
        val label = content.label

        if (label == null) {
            val message = "Nil label"
            println("routing.handler.LabelActionHandler: $message")
            call.respondText { message }
            return
        }

        if (!label.name.contains(labelIdentifier)) {
            val message = "Ignoring non xp- labels."
            println("routing.handler.LabelActionHandler: $message")
            call.respondText { message }
            return
        }

        if (issue == null) {
            call.respondText { "No issue found" }
            return
        }

        val otherRepoName = label.name.replace(labelIdentifier, "")
        val newIssue = NewIssue(
            title = issue.title,
            body = "Automatically generated from " + issue.html_url + "\n\n" + issue.body
        )

        val otherRepoUrl = getCrossPostingRepoApiUrl(issue.url, otherRepoName) + "/issues"
        GithubClient.postToUrl(otherRepoUrl, newIssue)
        println("routing.handler.LabelActionHandler: Created a new issue in $otherRepoName")
        call.respondText { "yay" }
    }

    private fun verifyGitHub(req: ApplicationRequest): Boolean {
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

    companion object {
        const val labelAction = "labeled"
        const val labelIdentifier = "xp-"
    }
}