/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.detail.timeline.helper

import im.vector.app.core.extensions.localDateTime
import im.vector.app.core.resources.UserPreferencesProvider
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.getRelationContent
import org.matrix.android.sdk.api.session.events.model.getRootThreadEventId
import org.matrix.android.sdk.api.session.events.model.isThread
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class TimelineEventVisibilityHelper @Inject constructor(private val userPreferencesProvider: UserPreferencesProvider) {

    /**
     * @param timelineEvents the events to search in
     * @param index the index to start computing (inclusive)
     * @param minSize the minimum number of same type events to have sequentially, otherwise will return an empty list
     * @param eventIdToHighlight used to compute visibility
     *
     * @return a list of timeline events which have sequentially the same type following the next direction.
     */
    private fun nextSameTypeEvents(
            timelineEvents: List<TimelineEvent>,
            index: Int,
            minSize: Int,
            eventIdToHighlight: String?,
            rootThreadEventId: String?,
            isFromThreadTimeline: Boolean): List<TimelineEvent> {
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
        val indexOfFirstDifferentEventType = nextSameDayEvents.indexOfFirst { it.root.getClearType() != timelineEvent.root.getClearType() }
        val sameTypeEvents = if (indexOfFirstDifferentEventType == -1) {
            nextSameDayEvents
        } else {
            nextSameDayEvents.subList(0, indexOfFirstDifferentEventType)
        }
        val filteredSameTypeEvents = sameTypeEvents.filter {
            shouldShowEvent(
                    timelineEvent = it,
                    highlightedEventId = eventIdToHighlight,
                    isFromThreadTimeline = isFromThreadTimeline,
                    rootThreadEventId = rootThreadEventId
            )
        }
        if (filteredSameTypeEvents.size < minSize) {
            return emptyList()
        }
        return filteredSameTypeEvents
    }

    /**
     * @param timelineEvents the events to search in
     * @param index the index to start computing (inclusive)
     * @param minSize the minimum number of same type events to have sequentially, otherwise will return an empty list
     * @param eventIdToHighlight used to compute visibility
     *
     * @return a list of timeline events which have sequentially the same type following the prev direction.
     */
    fun prevSameTypeEvents(
            timelineEvents: List<TimelineEvent>,
            index: Int,
            minSize: Int,
            eventIdToHighlight: String?,
            rootThreadEventId: String?,
            isFromThreadTimeline: Boolean): List<TimelineEvent> {
        val prevSub = timelineEvents.subList(0, index + 1)
        return prevSub
                .reversed()
                .let {
                    nextSameTypeEvents(it, 0, minSize, eventIdToHighlight, rootThreadEventId, isFromThreadTimeline)
                }
    }

    /**
     * @param timelineEvent the event to check for visibility
     * @param highlightedEventId can be checked to force visibility to true
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

        // Allow only the the threads within the rootThreadEventId along with the root event
        if (userPreferencesProvider.areThreadMessagesEnabled() && isFromThreadTimeline) {
            return if (root.getRootThreadEventId() == rootThreadEventId) {
                false
            } else root.eventId != rootThreadEventId
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
