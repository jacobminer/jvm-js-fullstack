package services.digest

import models.DigestComment
import models.TimelineItem

abstract class DigestEventHelper(private val changeType: String, private val allowedTypes: List<String>) {
    fun handleTimelineEvents(timelineEvents: List<TimelineItem>): List<TimelineItem> {
        return timelineEvents
            .filter { allowedTypes.contains(it.event) }
            .map {
                it.changeType = changeType
                it
            }
    }
    fun createChangeComment(changeEvent: TimelineItem): DigestComment? {
        if (changeEvent.changeType != this.changeType) return null
        return generateComment(changeEvent);
    }
    abstract fun generateComment(changeEvent: TimelineItem): DigestComment?
}