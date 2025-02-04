/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import im.vector.app.core.extensions.getVectorLastMessageContent
import im.vector.app.features.home.room.detail.timeline.item.PollResponseData
import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.PollCreationInfo
import org.matrix.android.sdk.api.session.room.model.message.PollQuestion
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

object RoomPollFixture {

    fun givenATimelineEvent(
            eventId: String,
            roomId: String,
            creationTimestamp: Long,
            content: MessageContent,
    ): TimelineEvent {
        val timelineEvent = mockk<TimelineEvent>()
        every { timelineEvent.root.eventId } returns eventId
        every { timelineEvent.roomId } returns roomId
        every { timelineEvent.root.originServerTs } returns creationTimestamp
        every { timelineEvent.getVectorLastMessageContent() } returns content
        return timelineEvent
    }

    fun givenAMessagePollContent(pollCreationInfo: PollCreationInfo): MessagePollContent {
        return MessagePollContent(
                unstablePollCreationInfo = pollCreationInfo,
        )
    }

    fun givenPollCreationInfo(pollTitle: String): PollCreationInfo {
        return PollCreationInfo(
                question = PollQuestion(unstableQuestion = pollTitle),
        )
    }

    fun givenAPollResponseData(isClosed: Boolean, totalVotes: Int): PollResponseData {
        return PollResponseData(
                myVote = "",
                votes = emptyMap(),
                isClosed = isClosed,
                totalVotes = totalVotes,
        )
    }
}
