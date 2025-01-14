/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.detail.ui

import im.vector.app.core.extensions.getVectorLastMessageContent
import im.vector.app.features.home.room.detail.timeline.factory.PollItemViewStateFactory
import im.vector.app.features.home.room.detail.timeline.helper.PollResponseDataFactory
import im.vector.app.features.home.room.detail.timeline.item.PollResponseData
import im.vector.app.features.roomprofile.polls.detail.domain.GetEndedPollEventIdUseCase
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import timber.log.Timber
import javax.inject.Inject

class RoomPollDetailMapper @Inject constructor(
        private val pollResponseDataFactory: PollResponseDataFactory,
        private val pollItemViewStateFactory: PollItemViewStateFactory,
        private val getEndedPollEventIdUseCase: GetEndedPollEventIdUseCase,
) {

    fun map(timelineEvent: TimelineEvent): RoomPollDetail? {
        val eventId = timelineEvent.root.eventId.orEmpty()
        val result = runCatching {
            val content = timelineEvent.getVectorLastMessageContent()
            val pollResponseData = pollResponseDataFactory.create(timelineEvent)
            val creationTimestamp = timelineEvent.root.originServerTs ?: 0
            return if (eventId.isNotEmpty() && creationTimestamp > 0 && content is MessagePollContent) {
                val isPollEnded = pollResponseData?.isClosed.orFalse()
                val endedPollEventId = getEndedPollEventId(
                        isPollEnded,
                        startPollEventId = eventId,
                        roomId = timelineEvent.roomId,
                )
                convertToRoomPollDetail(
                        creationTimestamp = creationTimestamp,
                        content = content,
                        pollResponseData = pollResponseData,
                        isPollEnded = isPollEnded,
                        endedPollEventId = endedPollEventId,
                )
            } else {
                Timber.w("missing mandatory info about poll event with id=$eventId")
                null
            }
        }

        if (result.isFailure) {
            Timber.w("failed to map event with id $eventId")
        }
        return result.getOrNull()
    }

    private fun convertToRoomPollDetail(
            creationTimestamp: Long,
            content: MessagePollContent,
            pollResponseData: PollResponseData?,
            isPollEnded: Boolean,
            endedPollEventId: String?,
    ): RoomPollDetail {
        // we assume the poll has been sent
        val pollItemViewState = pollItemViewStateFactory.create(
                pollContent = content,
                pollResponseData = pollResponseData,
                isSent = true,
        )
        return RoomPollDetail(
                creationTimestamp = creationTimestamp,
                isEnded = isPollEnded,
                pollItemViewState = pollItemViewState,
                endedPollEventId = endedPollEventId,
        )
    }

    private fun getEndedPollEventId(
            isPollEnded: Boolean,
            startPollEventId: String,
            roomId: String,
    ): String? {
        return if (isPollEnded) {
            getEndedPollEventIdUseCase.execute(startPollEventId = startPollEventId, roomId = roomId)
        } else {
            null
        }
    }
}
