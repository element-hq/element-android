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
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class TimelineEventVisibilityHelper @Inject constructor(private val userPreferencesProvider: UserPreferencesProvider) {

    fun nextSameTypeEvents(timelineEvents: List<TimelineEvent>, index: Int, minSize: Int): List<TimelineEvent> {
        if (index >= timelineEvents.size - 1) {
            return emptyList()
        }
        val timelineEvent = timelineEvents[index]
        val nextSubList = timelineEvents.subList(index + 1, timelineEvents.size)
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
        val filteredSameTypeEvents = sameTypeEvents.filter { shouldShowEvent(it)}
        if (filteredSameTypeEvents.size < minSize) {
            return emptyList()
        }
        return  filteredSameTypeEvents
    }

    fun prevSameTypeEvents(timelineEvents: List<TimelineEvent>, index: Int, minSize: Int): List<TimelineEvent> {
        val prevSub = timelineEvents.subList(0, index + 1)
        return prevSub
                .reversed()
                .let {
                    nextSameTypeEvents(it, 0, minSize)
                }
                .reversed()
    }

    fun shouldShowEvent(timelineEvent: TimelineEvent, highlightEventId: String? = null): Boolean {
        // If show hidden events is true we should always display something
        if (userPreferencesProvider.shouldShowHiddenEvents()) {
            return true
        }
        // We always show highlighted event
        if (timelineEvent.eventId == highlightEventId) {
            return true
        }
        if (!timelineEvent.isDisplayable()) {
            return false
        }
        // Check for special case where we should hide the event, like redacted, relation, memberships... according to user preferences.
        return !timelineEvent.shouldBeHidden()
    }

    private fun TimelineEvent.isDisplayable(): Boolean {
        return TimelineDisplayableEvents.DISPLAYABLE_TYPES.contains(root.getClearType())
    }

    private fun TimelineEvent.shouldBeHidden(): Boolean {
        if (root.isRedacted() && !userPreferencesProvider.shouldShowRedactedMessages()) {
            return true
        }
        if (root.getRelationContent()?.type == RelationType.REPLACE) {
            return true
        }
        if (root.getClearType() == EventType.STATE_ROOM_MEMBER) {
            val diff = computeMembershipDiff()
            if ((diff.isJoin || diff.isPart) && !userPreferencesProvider.shouldShowRoomMemberStateEvents()) return true
        }
        return false
    }

    private fun TimelineEvent.computeMembershipDiff(): MembershipDiff {
        val content = root.getClearContent().toModel<RoomMemberContent>()
        val prevContent = root.resolvedPrevContent().toModel<RoomMemberContent>()

        val isMembershipChanged = content?.membership != prevContent?.membership;
        val isJoin = isMembershipChanged && content?.membership == Membership.JOIN
        val isPart = isMembershipChanged && content?.membership == Membership.LEAVE && root.stateKey == root.senderId

        val isJoinToJoin = !isMembershipChanged && content?.membership == Membership.JOIN
        val isDisplaynameChange = isJoinToJoin && content?.displayName != prevContent?.displayName;
        val isAvatarChange = isJoinToJoin && content?.avatarUrl !== prevContent?.avatarUrl

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
