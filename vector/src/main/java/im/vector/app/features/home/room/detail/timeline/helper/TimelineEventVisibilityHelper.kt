/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.helper

import im.vector.app.core.extensions.localDateTime
import im.vector.app.core.resources.UserPreferencesProvider
import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.isVoiceBroadcast
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.getRelationContent
import org.matrix.android.sdk.api.session.events.model.getRootThreadEventId
import org.matrix.android.sdk.api.session.events.model.isThread
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.localecho.RoomLocalEcho
import org.matrix.android.sdk.api.session.room.model.message.asMessageAudioEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class TimelineEventVisibilityHelper @Inject constructor(
        private val userPreferencesProvider: UserPreferencesProvider,
) {

    private interface PredicateToStopSearch {
        /**
         * Indicate whether a search on events should stop by comparing 2 given successive events.
         * @param oldEvent the oldest event between the 2 events to compare
         * @param newEvent the more recent event between the 2 events to compare
         */
        fun shouldStopSearch(oldEvent: Event, newEvent: Event): Boolean
    }

    /**
     * @param timelineEvents the events to search in, sorted from oldest event to newer event
     * @param index the index to start computing (inclusive)
     * @param minSize the minimum number of same type events to have sequentially, otherwise will return an empty list
     * @param eventIdToHighlight used to compute visibility
     * @param rootThreadEventId the root thread event id if in a thread timeline
     * @param isFromThreadTimeline true if the timeline is a thread
     * @param predicateToStop events are taken until this condition is met
     *
     * @return a list of timeline events which meet sequentially the same criteria following the next direction.
     */
    private fun nextEventsUntil(
            timelineEvents: List<TimelineEvent>,
            index: Int,
            minSize: Int,
            eventIdToHighlight: String?,
            rootThreadEventId: String?,
            isFromThreadTimeline: Boolean,
            predicateToStop: PredicateToStopSearch
    ): List<TimelineEvent> {
        if (index >= timelineEvents.size - 1) {
            return emptyList()
        }
        val timelineEvent = timelineEvents[index]
        val nextSubList = timelineEvents.subList(index, timelineEvents.size)
        val indexOfNextDay = nextSubList.indexOfFirst {
            val date = it.root.localDateTime()
            val nextDate = timelineEvent.root.localDateTime()
            date.toLocalDate() != nextDate.toLocalDate()
        }
        val nextSameDayEvents = if (indexOfNextDay == -1) {
            nextSubList
        } else {
            nextSubList.subList(0, indexOfNextDay)
        }
        val indexOfFirstDifferentEvent = nextSameDayEvents.indexOfFirst {
            predicateToStop.shouldStopSearch(oldEvent = timelineEvent.root, newEvent = it.root)
        }
        val similarEvents = if (indexOfFirstDifferentEvent == -1) {
            nextSameDayEvents
        } else {
            nextSameDayEvents.subList(0, indexOfFirstDifferentEvent)
        }
        val filteredSimilarEvents = similarEvents.filter {
            shouldShowEvent(
                    timelineEvent = it,
                    highlightedEventId = eventIdToHighlight,
                    isFromThreadTimeline = isFromThreadTimeline,
                    rootThreadEventId = rootThreadEventId
            )
        }
        return if (filteredSimilarEvents.size < minSize) emptyList() else filteredSimilarEvents
    }

    /**
     * @param timelineEvents the events to search in, sorted from newer event to oldest event
     * @param index the index to start computing (inclusive)
     * @param minSize the minimum number of same type events to have sequentially, otherwise will return an empty list
     * @param eventIdToHighlight used to compute visibility
     * @param rootThreadEventId the root thread eventId
     * @param isFromThreadTimeline true if the timeline is a thread
     *
     * @return a list of timeline events which have sequentially the same type following the prev direction.
     */
    fun prevSameTypeEvents(
            timelineEvents: List<TimelineEvent>,
            index: Int,
            minSize: Int,
            eventIdToHighlight: String?,
            rootThreadEventId: String?,
            isFromThreadTimeline: Boolean
    ): List<TimelineEvent> {
        val prevSub = timelineEvents.subList(0, index + 1)
        return prevSub
                .reversed()
                .let {
                    nextEventsUntil(it, 0, minSize, eventIdToHighlight, rootThreadEventId, isFromThreadTimeline, object : PredicateToStopSearch {
                        override fun shouldStopSearch(oldEvent: Event, newEvent: Event): Boolean {
                            return oldEvent.getClearType() != newEvent.getClearType()
                        }
                    })
                }
    }

    /**
     * @param timelineEvents the events to search in, sorted from newer event to oldest event
     * @param index the index to start computing (inclusive)
     * @param minSize the minimum number of same type events to have sequentially, otherwise will return an empty list
     * @param eventIdToHighlight used to compute visibility
     * @param rootThreadEventId the root thread eventId
     * @param isFromThreadTimeline true if the timeline is a thread
     *
     * @return a list of timeline events which are all redacted following the prev direction.
     */
    fun prevRedactedEvents(
            timelineEvents: List<TimelineEvent>,
            index: Int,
            minSize: Int,
            eventIdToHighlight: String?,
            rootThreadEventId: String?,
            isFromThreadTimeline: Boolean
    ): List<TimelineEvent> {
        val prevDisplayableEvents = timelineEvents.subList(0, index + 1)
                .filter {
                    shouldShowEvent(
                            timelineEvent = it,
                            highlightedEventId = eventIdToHighlight,
                            isFromThreadTimeline = isFromThreadTimeline,
                            rootThreadEventId = rootThreadEventId)
                }
        return prevDisplayableEvents
                .reversed()
                .let {
                    nextEventsUntil(it, 0, minSize, eventIdToHighlight, rootThreadEventId, isFromThreadTimeline, object : PredicateToStopSearch {
                        override fun shouldStopSearch(oldEvent: Event, newEvent: Event): Boolean {
                            return !newEvent.isRedacted()
                        }
                    })
                }
    }

    /**
     * @param timelineEvent the event to check for visibility
     * @param highlightedEventId can be checked to force visibility to true
     * @param isFromThreadTimeline true if the timeline is a thread
     * @param rootThreadEventId if this param is null it means we are in the original timeline
     * @return true if the event should be shown in the timeline.
     */
    fun shouldShowEvent(
            timelineEvent: TimelineEvent,
            highlightedEventId: String?,
            isFromThreadTimeline: Boolean,
            rootThreadEventId: String?
    ): Boolean {
        // If show hidden events is true we should always display something
        if (userPreferencesProvider.shouldShowHiddenEvents() && !isFromThreadTimeline) {
            return true
        }
        // We always show highlighted event
        if (timelineEvent.eventId == highlightedEventId) {
            return true
        }
        if (!timelineEvent.isDisplayable()) {
            return false
        }

        // Check for special case where we should hide the event, like redacted, relation, memberships... according to user preferences.
        return !timelineEvent.shouldBeHidden(rootThreadEventId, isFromThreadTimeline)
    }

    private fun TimelineEvent.isDisplayable(): Boolean {
        return TimelineDisplayableEvents.DISPLAYABLE_TYPES.contains(root.getClearType())
    }

    private fun TimelineEvent.shouldBeHidden(rootThreadEventId: String?, isFromThreadTimeline: Boolean): Boolean {
        if (root.isRedacted() && !userPreferencesProvider.shouldShowRedactedMessages() && root.threadDetails?.isRootThread == false) {
            return true
        }

        // We should not display deleted thread messages within the normal timeline
        if (root.isRedacted() &&
                userPreferencesProvider.areThreadMessagesEnabled() &&
                !isFromThreadTimeline &&
                (root.isThread() || root.threadDetails?.isThread == true)) {
            return true
        }
        if (root.isRedacted() &&
                !userPreferencesProvider.shouldShowRedactedMessages() &&
                userPreferencesProvider.areThreadMessagesEnabled() &&
                isFromThreadTimeline &&
                root.isThread()) {
            return true
        }

        if (root.getRelationContent()?.type == RelationType.REPLACE) {
            return true
        }
        if (root.getClearType() == EventType.STATE_ROOM_MEMBER) {
            val diff = computeMembershipDiff()
            if ((diff.isJoin || diff.isPart) && !userPreferencesProvider.shouldShowJoinLeaves()) return true
            if ((diff.isAvatarChange || diff.isDisplaynameChange) && !userPreferencesProvider.shouldShowAvatarDisplayNameChanges()) return true
        }

        if (userPreferencesProvider.areThreadMessagesEnabled() && !isFromThreadTimeline && root.isThread()) {
            return true
        }

        // Hide fake events for local rooms
        if (RoomLocalEcho.isLocalEchoId(roomId) &&
                (root.getClearType() == EventType.STATE_ROOM_MEMBER ||
                        root.getClearType() == EventType.STATE_ROOM_HISTORY_VISIBILITY ||
                        root.getClearType() == EventType.STATE_ROOM_THIRD_PARTY_INVITE)) {
            return true
        }

        // Allow only the the threads within the rootThreadEventId along with the root event
        if (userPreferencesProvider.areThreadMessagesEnabled() && isFromThreadTimeline) {
            return if (root.getRootThreadEventId() == rootThreadEventId) {
                false
            } else root.eventId != rootThreadEventId
        }

        if (root.getClearType() in EventType.BEACON_LOCATION_DATA.values) {
            return !root.isRedacted()
        }

        if (root.getClearType() == VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO &&
                root.asVoiceBroadcastEvent()?.content?.voiceBroadcastState !in arrayOf(VoiceBroadcastState.STARTED, VoiceBroadcastState.STOPPED)) {
            return true
        }

        if (root.asMessageAudioEvent().isVoiceBroadcast()) {
            return true
        }

        return false
    }

    private fun TimelineEvent.computeMembershipDiff(): MembershipDiff {
        val content = root.getClearContent().toModel<RoomMemberContent>()
        val prevContent = root.resolvedPrevContent().toModel<RoomMemberContent>()

        val isMembershipChanged = content?.membership != prevContent?.membership
        val isJoin = isMembershipChanged && content?.membership == Membership.JOIN
        val isPart = isMembershipChanged && content?.membership == Membership.LEAVE && root.stateKey == root.senderId

        val isProfileChanged = !isMembershipChanged && content?.membership == Membership.JOIN
        val isDisplaynameChange = isProfileChanged && content?.displayName != prevContent?.displayName
        val isAvatarChange = isProfileChanged && content?.avatarUrl !== prevContent?.avatarUrl

        return MembershipDiff(
                isJoin = isJoin,
                isPart = isPart,
                isDisplaynameChange = isDisplaynameChange,
                isAvatarChange = isAvatarChange
        )
    }

    private data class MembershipDiff(
            val isJoin: Boolean,
            val isPart: Boolean,
            val isDisplaynameChange: Boolean,
            val isAvatarChange: Boolean
    )
}
