package services.digest

import Constants
import client.GithubClient
import kotlinx.serialization.Serializable
import logging.log
import models.*
import services.digest.digest_helpers.ClosedEventHelper
import services.digest.digest_helpers.LabelEventHelper
import services.digest.digest_helpers.MilestoneEventHelper
import java.time.Duration
import java.time.Instant

@Serializable
data class Repository(val issues_url: String, val name: String, val has_issues: Boolean? = null)

object DigestService {
    private val digestestableRepos = mutableListOf<String>()
    private var digestDeployTime = Long.MAX_VALUE

    init {
        updateDigestDeployTime()
        updateDigestibleRepos()
    }

    suspend fun beginDigest() {
        digestAllEnabledRepos(System.getenv("ORG_API_LINK"))
    }

    private fun updateDigestDeployTime() {
        val digestDeployTimeEnvVariable = System.getenv("ignoreIssuesBeforeEpochMillis")?.toLongOrNull()
        if (digestDeployTimeEnvVariable == null) {
            log.debug("Digest", "No digestDeployTime env variable set, ignoring all issues.")
            return
        }
        digestDeployTime = digestDeployTimeEnvVariable
        log.debug("Digest", "Set deploy time is $digestDeployTime. Ignoring all issues before deploy time.")
    }

    private fun updateDigestibleRepos() {
        val digestReposVariable = System.getenv("digestRepos")
        if (digestReposVariable == null) {
            log.debug("Digest", "No digestRepos env variable set.")
            return
        }
        val digestestableReposString = digestReposVariable.replace(Regex("/\\s/g"), "")
        if (digestestableReposString.isEmpty()) {
            log.debug("Digest", "DigestRepos is empty")
            return
        }
        log.debug("Digest", "Digestable repositories are $digestestableReposString")
        digestestableRepos.addAll(digestestableReposString.split(","))
    }

    private val digestEventsHelpers = listOf(
        MilestoneEventHelper(), LabelEventHelper(), ClosedEventHelper()
    )

    suspend fun debugDigestIssue(issueUrl: String) {
        log.debug("Digest", "Downloading issue from $issueUrl")
        val issue = GithubClient.getFromUrl<Issue>(issueUrl)
        if (issue == null) {
            log.debug("Digest", "Failed to download issue.")
            return
        }
        log.debug("Digest", "Downloaded issue.")
        digestIssue(issue)
    }

    suspend fun debugDigestRepo(repoUrl: String) {
        log.debug("Digest", "Downloading repo from $repoUrl")
        val repo = GithubClient.getFromUrl<Repository>(repoUrl)
        if (repo == null) {
            log.debug("Digest", "Failed to download repo")
            return
        }
        digestRepo(repo)
    }

    suspend fun debugDigestAllRepos(userUrl: String) {
        log.debug("Digest", "Downloading repos from $userUrl")
        digestAllEnabledRepos(userUrl)
    }

    private suspend fun digestAllEnabledRepos(userUrl: String) {
        updateDigestibleRepos()

        if (digestestableRepos.isEmpty()) {
            log.debug("Digest", "Not digesting any repos.")
            return
        }

        log.debug("Digest", "Digesting repos for user: $userUrl")
        getRepoWithIssues(userUrl)
            .filter { repo -> digestestableRepos.any { it == repo.name } }
            .forEach {
                log.debug("Digest", "Digesting repo ${it.name}")
                digestRepo(it)
            }
    }

    private suspend fun digestRepo(repo: Repository) {
        // also add the state=all in order to get closed issues
        val allIssuesUrl = repo.issues_url.replace("{/number}", "?state=all")
        log.debug("Digest", "Getting valid XP issues for ${repo.name}")
        val issues = getValidXpIssuesForRepo(allIssuesUrl)
        log.debug("Digest", "Got ${issues.count()} XP issues from ${repo.name}")
        for (issue in issues) {
            digestIssue(issue)
        }
    }

    private suspend fun getValidXpIssuesForRepo(issuesUrl: String): List<Issue> {
        updateDigestDeployTime()
        val issues = GithubClient.getFromUrl<List<Issue>>("$issuesUrl&per_page=10000")
        if (issues == null) {
            log.debug("Digest", "No issues available")
            return listOf()
        }

        log.debug("Digest", "Got ${issues.count()} issues total")
        return issues.filter {
            // filter out issues missing the xp- label and issues that haven't been updated in over 24 hours.
            val hasXpLabel = it.labels.firstOrNull { label ->
                val includesLabel = label.name.contains(Constants.labelIdentifier)
                val lastUpdateDate = Instant.parse(it.updated_at)
                val afterDeployDate = isAfterDeployDate(lastUpdateDate)
                if (includesLabel) {
                    log.debug("Digest", "Last update date = $lastUpdateDate")
                    log.debug("Digest", "After deploy date = $afterDeployDate")
                }
                includesLabel && afterDeployDate
            }
            hasXpLabel != null
        }
    }

    private fun isAfterDeployDate(lastUpdateDate: Instant): Boolean {
        return lastUpdateDate.toEpochMilli() >= digestDeployTime
    }

    private suspend fun getRepoWithIssues(userUrl: String): List<Repository> {
        val repoList = GithubClient.getFromUrl<List<Repository>>("$userUrl/repos?per_page=1000")
        if (repoList == null) {
            log.debug("Digest", "No repositories available")
            return listOf()
        }

        // filter out repos that don't have issues
        return repoList.filter { it.has_issues == true }
    }

    private suspend fun digestIssue(issue: Issue) {
        log.debug("Digest", "Digesting issue $issue")

        val xpLabel = issue.labels.firstOrNull { it.name.contains(Constants.labelIdentifier) }
        if (xpLabel == null) {
            log.debug("Digest", "Issue doesn't have ${Constants.labelIdentifier} label, skipping.")
            return
        }

        log.debug("Digest", "Found xp- label: $xpLabel")

        // get all source comments
        val comments = GithubClient.getFromUrl<List<Comment>>(issue.comments_url)
        if (comments == null) {
            log.debug("Digest", "No comments")
            return
        }

        val crossPostedComments = getCrossPostedCommentsFromIssue(issue.url)

        // find the last comment including the digest identifier
        val lastDigestComment = crossPostedComments?.lastOrNull { it.body?.contains(Constants.digestIdentifier) == true }

        if (lastDigestComment == null) {
            log.debug("Digest", "No previous digest comments")
            // creating first digest comment

            // get all label changes
            val timelineChangeEvents = getRelevantTimelineEvents(issue, null)
            postDigest(issue, xpLabel, comments, timelineChangeEvents)
        } else {
            log.debug("Digest", "Found previous digest comment")
            // creating new digest comment with updates since a previous one

            // get all label changes after digest event
            // get the last issue referenced in the digest event

            // handle digest legacy comments
            val lastChangeUrl = if (lastDigestComment.body?.contains("Last change:") == true) {
                log.debug("Digest", "Using last change url legacy parsing")
                lastDigestComment.body.split("Last change: ")[1]
            } else {
                getUrlFromLastDigestComment(lastDigestComment)
            }

            if (lastChangeUrl == null) {
                log.debug("DigestService", "Didn't find last change url")
                return
            }

            log.debug("Digest", "get from url: $lastChangeUrl")
            val changeJson = GithubClient.getFromUrl<Comment>(lastChangeUrl)
            val change = changeJson
            if (change == null) {
                log.debug("Digest", "Change undefined")
                return
            }

            val date = change.updated_at ?: change.created_at
            val lastDigestCommentDate = Instant.parse(date)
            val timelineChangeEvents = getRelevantTimelineEvents(issue, lastDigestCommentDate.toEpochMilli())
            val newComments = comments.filter {
                val time = Instant.parse(it.updated_at)
                isTimeWithin24Hours(time) && isAfterDate(time, lastDigestCommentDate.toEpochMilli())
            }
            postDigest(issue, xpLabel, newComments, timelineChangeEvents)
        }
    }

    private fun getUrlFromLastDigestComment(lastComment: Comment): String? {
        // last comment should contain "[xp-daily-digest](https://api.github.com/repos/jacobminer/A/issues/events/1943945073)\r\nOTHER CONTENT HERE"
        // split out into two parts based on xp-daily-digest]
        val parts = lastComment.body?.split("[${Constants.digestIdentifier}]") ?: return null
        // grab only the end part (https://api.github.com/repos/jacobminer/A/issues/events/1943945073)\r\nOTHER CONTENT HERE
        val urlComponent = parts[1]
        // remove first bracket https://api.github.com/repos/jacobminer/A/issues/events/1943945073)\r\nOTHER CONTENT HERE
        val startOfLink = urlComponent.replace("(", "")
        // Split into two parts based on )
        val linkAndContent = startOfLink.split(")")
        // Grab the second part, which should only be the link https://api.github.com/repos/jacobminer/A/issues/events/1943945073
        val link = linkAndContent[0]
        // trim for good measure
        return link.trim()
    }

    private suspend fun postDigest(sourceIssue: Issue, xpLabel: Label, comments: List<Comment>, timelineChangeEvents: List<TimelineItem>) {
        val digestBody = mutableListOf<DigestComment>()

        // loop through comments array, adding custom object date
        for (comment in comments) {
            val commenterName = comment.user?.login ?: continue
            val commentText = "$commenterName commented:\n> ${comment.body}\r\n"

            val digestComment = DigestComment(
                body = commentText,
                date = Instant.parse(comment.updated_at),
                url = comment.url
            )

            digestBody.add(digestComment)
        }

        for (changeEvent in timelineChangeEvents) {
            if (changeEvent.changeType == null) {
                log.debug("postDigest", "Change type is not defined!")
                continue
            }

            for (helper in digestEventsHelpers) {
                val comment = helper.createChangeComment(changeEvent)
                if (comment != null) {
                    digestBody.add(comment)
                }
            }
        }

        // sort digest by date
        digestBody.sortBy { it.date }

        if (digestBody.isEmpty()) {
            log.debug("Digest", "Nothing to digest, exiting.")
            return
        }

        // create a new array of digest comments
        val digestComments = digestBody.map { it.body }.toMutableList()

        val changesCount = digestBody.count()
        digestComments.add(0, "$changesCount ${if (changesCount > 1) "changes" else "change"} yesterday to linked issue ${sourceIssue.html_url}:\r\n")
        val lastDigestComment = digestBody[digestBody.count() - 1]
        digestComments.add(0, "[${Constants.digestIdentifier}](${lastDigestComment.url})\r\n")

        // join each comment with \n character
        val digestComment = digestComments.joinToString("\r\n")

        // post the comment
        val error = GithubClient.postCommentToCrossPostedIssue(xpLabel, sourceIssue, digestComment)
        if (error != null) {
            log.debug("Digest", error)
        }
    }

    private suspend fun getCrossPostedCommentsFromIssue(sourceIssueUrl: String): List<Comment>? {
        log.debug("Digest", "Getting cross posted comment for issue $sourceIssueUrl")
        val url = GithubClient.getCrossPostedIssueUrl(sourceIssueUrl) ?: return null
        val comments = GithubClient.getFromUrl<List<Comment>>("$url/comments")
        if (comments == null) {
            log.debug("Digest", "Comments are undefined")
            return listOf()
        }
        log.debug("Digest", "Found comments.")
        return comments
    }

    private suspend fun getRelevantTimelineEvents(issue: Issue, timeInMillis: Long?): List<TimelineItem> {
        log.debug("Digest", "Getting label changes for issue ${issue.url} since $timeInMillis")
        val timelineEvents = GithubClient.getTimeline(issue.url)

        if (timelineEvents == null) {
            log.debug("Digest", "Timeline returned undefined")
            return listOf()
        }

        val combinedEvents = mutableListOf<TimelineItem>()

        for (helper in digestEventsHelpers) {
            combinedEvents.addAll(helper.handleTimelineEvents(timelineEvents))
        }

        // always filter out events > 24 hours old
        val dailyFilteredEvents = combinedEvents.filter {
            val time = Instant.parse(it.created_at)
            isTimeWithin24Hours(time)
        }

        return if (timeInMillis == null) {
            log.debug("Digest", "Not using timeInMillis to filter ${dailyFilteredEvents.count()} events.")
            dailyFilteredEvents
        } else {
            log.debug("Digest", "Filtering ${dailyFilteredEvents.count()} events with $timeInMillis")
            // filter out all events before timeInMillis
            val filteredEvents = dailyFilteredEvents.filter {
                val time = Instant.parse(it.created_at)
                isAfterDate(time, timeInMillis)
            }
            log.debug("Digest", "Filtered events, count is ${filteredEvents.count()}")
            filteredEvents
        }
    }

    private fun isTimeWithin24Hours(date: Instant): Boolean {
        val currentDate = Instant.now()
        // heroku dyno restarts take place every 24 hours + up to 216 minutes, so we need to account for that.
        return Duration.between(currentDate, date).toHours() <= 28
    }

    private fun isAfterDate(time: Instant, timeInMillis: Long): Boolean {
        return time.isAfter(Instant.ofEpochMilli(timeInMillis))
    }
}