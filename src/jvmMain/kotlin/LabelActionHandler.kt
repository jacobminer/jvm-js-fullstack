import io.ktor.application.*
import io.ktor.response.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class NewIssue(val title: String?, val body: String)

class LabelActionHandler(private val labelIdentifier: String) {
    // Make sure you pass in the `url` not the `html_url`!
    private fun getCrossPostingRepoApiUrl(issueUrl: String, repoName: String): String {
        val currentRepoComponents = issueUrl.split("/", ignoreCase = true, limit = 100)
        val otherRepoComponents = currentRepoComponents.slice(0 until currentRepoComponents.count() - 3)
        val otherRepo = otherRepoComponents.joinToString("/") + "/" + repoName
        return otherRepo
    }

    suspend fun handleLabeledAction(payload: WebHookContent, call: ApplicationCall) {
        val issue = payload.issue
        val label = payload.label

        if (label == null) {
            val message = "Nil label"
            println("LabelActionHandler: $message")
            call.respondText { message }
            return
        }

        if (!label.name.contains(this.labelIdentifier)) {
            val message = "Ignoring non xp- labels."
            println("LabelActionHandler: $message")
            call.respondText { message }
            return
        }

        if (issue == null) {
            call.respondText { "No issue found" }
            return
        }

        val otherRepoName = label.name.replace(this.labelIdentifier, "")
        val newIssue = NewIssue(
            title = issue.title,
            body = "Automatically generated from " + issue.html_url + "\n\n" + issue.body
        )

        val otherRepoUrl = getCrossPostingRepoApiUrl(issue.url, otherRepoName) + "/issues"
        GithubClient.postToUrl(otherRepoUrl, newIssue)
        println("LabelActionHandler: Created a new issue in $otherRepoName")
        call.respondText { "yay" }
    }

    companion object {
        const val labelAction = "labeled"
    }
}