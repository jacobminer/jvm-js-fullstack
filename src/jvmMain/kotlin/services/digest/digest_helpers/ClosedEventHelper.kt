package services.digest.digest_helpers

import models.DigestComment
import models.TimelineItem
import services.digest.DigestEventHelper
import java.time.Instant

class ClosedEventHelper: DigestEventHelper("closed", listOf("closed")) {
    override fun generateComment(changeEvent: TimelineItem): DigestComment? {
        val closedEvent = changeEvent
        val actorName = closedEvent.actor?.login ?: return null
        val changeText = "$actorName **closed** issue.\r\n"
        return DigestComment(
            body = changeText,
            date = Instant.parse(closedEvent.created_at),
            url = closedEvent.url
        )
    }
}