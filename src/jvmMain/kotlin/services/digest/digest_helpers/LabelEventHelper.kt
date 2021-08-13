package services.digest.digest_helpers

import models.DigestComment
import models.TimelineItem
import services.digest.DigestEventHelper
import java.time.Instant

class LabelEventHelper: DigestEventHelper("label", listOf("labeled", "unlabeled")) {
    override fun generateComment(changeEvent: TimelineItem): DigestComment? {
        val labelChangeEvent = changeEvent
        val labelName = labelChangeEvent.label?.name ?: return null
        val actorName = labelChangeEvent.actor?.login ?: return null
        val addedText = actorName + " added label **${labelName}**\r\n"
        val removedText = actorName + " removed label **${labelName}**\r\n"
        val changeText = if (labelChangeEvent.event == "labeled") addedText else removedText
        val url = labelChangeEvent.url ?: return null
        return DigestComment(
            body = changeText,
            date = Instant.parse(labelChangeEvent.created_at),
            url = url
        )
    }
}