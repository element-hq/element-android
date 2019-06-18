/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotredesign.features.home.room.detail.timeline.util

import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.core.extensions.localDateTime
import im.vector.riotredesign.core.resources.ColorProvider
import im.vector.riotredesign.features.home.getColorFromUserId
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineDateFormatter
import im.vector.riotredesign.features.home.room.detail.timeline.helper.senderAvatar
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.riotredesign.features.home.room.detail.timeline.item.ReactionInfoData
import me.gujun.android.span.span

/**
 * This class compute if data of an event (such has avatar, display name, ...) should be displayed, depending on the previous event in the timeline
 */
class MessageInformationDataFactory(private val timelineDateFormatter: TimelineDateFormatter,
                                    private val colorProvider: ColorProvider) {

    fun create(event: TimelineEvent, nextEvent: TimelineEvent?): MessageInformationData {
        // Non nullability has been tested before
        val eventId = event.root.eventId!!

        val date = event.root.localDateTime()

        val nextDate = nextEvent?.root?.localDateTime()
        val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()
        val isNextMessageReceivedMoreThanOneHourAgo = nextDate?.isBefore(date.minusMinutes(60))
                ?: false
        val showInformation =
                addDaySeparator
                        || event.senderAvatar != nextEvent?.senderAvatar
                        || event.senderName != nextEvent?.senderName
                        || (nextEvent?.root?.getClearType() != EventType.MESSAGE && nextEvent?.root?.getClearType() != EventType.ENCRYPTED)
                        || isNextMessageReceivedMoreThanOneHourAgo

        val time = timelineDateFormatter.formatMessageHour(date)
        val avatarUrl = event.senderAvatar()
        val memberName = event.senderName ?: event.root.sender ?: ""
        val formattedMemberName = span(memberName) {
            textColor = colorProvider.getColor(getColorFromUserId(event.root.sender
                    ?: ""))
        }

        val hasBeenEdited = event.annotations?.editSummary != null

        return MessageInformationData(
                eventId = eventId,
                senderId = event.root.sender ?: "",
                sendState = event.sendState,
                time = time,
                avatarUrl = avatarUrl,
                memberName = formattedMemberName,
                showInformation = showInformation,
                orderedReactionList = event.annotations?.reactionsSummary?.map {
                    ReactionInfoData(it.key, it.count, it.addedByMe, it.localEchoEvents.isEmpty())
                },
                hasBeenEdited = hasBeenEdited
        )
    }
}