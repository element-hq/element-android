/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.list.ui

import im.vector.app.core.extensions.getVectorLastMessageContent
import im.vector.app.features.home.room.detail.timeline.factory.PollOptionViewStateFactory
import im.vector.app.features.home.room.detail.timeline.helper.PollResponseDataFactory
import im.vector.app.features.home.room.detail.timeline.item.PollResponseData
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import timber.log.Timber
import javax.inject.Inject

class PollSummaryMapper @Inject constructor(
        private val pollResponseDataFactory: PollResponseDataFactory,
        private val pollOptionViewStateFactory: PollOptionViewStateFactory,
) {

    fun map(timelineEvent: TimelineEvent): PollSummary? {
        val eventId = timelineEvent.root.eventId.orEmpty()
        val result = runCatching {
            val content = timelineEvent.getVectorLastMessageContent()
            val pollResponseData = pollResponseDataFactory.create(timelineEvent)
            val creationTimestamp = timelineEvent.root.originServerTs ?: 0
            return if (eventId.isNotEmpty() && creationTimestamp > 0 && content is MessagePollContent) {
                convertToPollSummary(
                        eventId = eventId,
                        creationTimestamp = creationTimestamp,
                        messagePollContent = content,
                        pollResponseData = pollResponseData
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

    private fun convertToPollSummary(
            eventId: String,
            creationTimestamp: Long,
            messagePollContent: MessagePollContent,
            pollResponseData: PollResponseData?
    ): PollSummary {
        val pollCreationInfo = messagePollContent.getBestPollCreationInfo()
        val pollTitle = pollCreationInfo?.question?.getBestQuestion().orEmpty()
        return if (pollResponseData?.isClosed == true) {
            PollSummary.EndedPoll(
                    id = eventId,
                    creationTimestamp = creationTimestamp,
                    title = pollTitle,
                    totalVotes = pollResponseData.totalVotes,
                    winnerOptions = pollOptionViewStateFactory
                            .createPollEndedOptions(pollCreationInfo, pollResponseData)
                            .filter { it.isWinner },
            )
        } else {
            PollSummary.ActivePoll(
                    id = eventId,
                    creationTimestamp = creationTimestamp,
                    title = pollTitle,
            )
        }
    }
}
