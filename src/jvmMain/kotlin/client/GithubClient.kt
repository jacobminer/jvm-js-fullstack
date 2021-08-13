package client

import Constants
import JsonConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import logging.log
import models.Issue
import models.Label
import models.NewComment
import models.TimelineItem

object GithubClient {
    private lateinit var _client: HttpClient
    fun getClient(): HttpClient {
        if (!this::_client.isInitialized) {
            _client = HttpClient(CIO) {
                install(JsonFeature) {
                    serializer = KotlinxSerializer(JsonConfig.json)
                }
            }
        }
        return _client
    }

    fun getUserAgent() = "steamclockkmp-bot"
    fun getToken() = "token ${System.getenv("crossplat")}"

    suspend fun <T: Any> postToUrl(url: String, content: T): HttpResponse? {
        log.debug("postToUrl", "Calling POST to $url")
        return try {
            getClient().post(url) {
                contentType(ContentType.Application.Json)
                body = content
                headers {
                    append("User-Agent", getUserAgent())
                    append("Authorization", getToken())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend inline fun <reified T: Any> getFromUrl(issueUrl: String, acceptHeader: String? = null): T? {
        log.debug("getFromUrl", "Calling GET for url = $issueUrl")
        return try {
            getClient().get(issueUrl) {
                contentType(ContentType.Application.Json)
                headers {
                    if (acceptHeader != null)
                        append("Accept", acceptHeader)
                    append("User-Agent", getUserAgent())
                    append("Authorization", getToken())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getTimeline(issueUrl: String): List<TimelineItem>? {
        val timelineItems = getFromUrl<List<TimelineItem>>("$issueUrl/timeline", "application/vnd.github.mockingbird-preview")
        if (timelineItems == null) {
            log.debug("GithubNetworkingHelper", "No timeline items found")
            return null
        }
        return timelineItems
    }

    suspend fun getCrossPostedIssueUrl(issueUrl: String): String? {
        log.debug("GithubNetworkingHelper", "Url = $issueUrl/timeline")
        val currentRepo = issueUrl.split("/")[5]

        val timelineItems = getTimeline(issueUrl) ?: listOf()

        // get the reference to issue B for issue A
        for (timelineItem in timelineItems) {
            if (timelineItem.actor?.login != "steamclock-bot") continue

            if (timelineItem.source == null) {
                log.debug("GithubNetworkingHelper", "source is undefined")
                continue
            }

            val sourceType = timelineItem.source.type
            if (!sourceType.contains("issue")) {
                log.debug("GithubNetworkingHelper", "source type isn't issue.")
                continue
            }

            // note: due to a change in the github api, we now need to replace the repo in the source.issue.url with the repo from the source.issue.html_url
            val targetRepo = timelineItem.source.issue.html_url.split("/")[4]
            val timelineIssueReference = timelineItem.source.issue
            val timelineReferenceUrl = timelineIssueReference.url.replace(currentRepo, targetRepo)

            log.debug("GithubNetworkingHelper", "Found timeline issue reference $timelineReferenceUrl for issueUrl $issueUrl")
            return timelineReferenceUrl
        }

        log.debug("GithubNetworkingHelper", "Failed to find correct timeline issue for $issueUrl")
        return null
    }

    suspend fun postCommentToCrossPostedIssue(label: Label, repoIssue: Issue?, comment: String): String? {
        if (repoIssue == null) {
            val message = "Ignoring because issue isn't available."
            log.debug("GithubNetworkingHelper", message)
            return message
        }

        val repoName = label.name.replace(Constants.labelIdentifier, "")

        val url = getCrossPostedIssueUrl(repoIssue.url)
        if (url === null) {
            val message = "Ignoring because url isn't available."
            log.debug("GithubNetworkingHelper", message)
            return message
        }
        log.debug("GithubNetworkingHelper", "Adding comment to $url")
        postToUrl("$url/comments", NewComment(body = comment))
        log.debug("GithubNetworkingHelper", "Created a new comment in $repoName")
        return null
    }
}