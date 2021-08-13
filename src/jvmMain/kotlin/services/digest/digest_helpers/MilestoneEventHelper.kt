package services.digest.digest_helpers

import models.DigestComment
import models.TimelineItem
import services.digest.DigestEventHelper
import java.time.Instant

class MilestoneEventHelper: DigestEventHelper("milestone", listOf("milestoned", "demilestoned")) {
    override fun generateComment(changeEvent: TimelineItem): DigestComment? {
        val milestoneName = changeEvent.milestone?.title ?: return null
        val actorName = changeEvent.actor?.login ?: return null
        val addedText = actorName + " assigned milestone **${milestoneName}**\r\n"
        val removedText = actorName + " unassigned milestone **${milestoneName}**\r\n"
        val changeText = if (changeEvent.event == "milestoned") addedText else removedText
        return DigestComment(
            body = changeText,
            date = Instant.parse(changeEvent.created_at),
            url = changeEvent.url
        )
    }
}