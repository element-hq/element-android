/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.app.features.home.room.detail.timeline.item.PollResponseData
import im.vector.app.features.home.room.detail.timeline.item.ReactionsSummaryData
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.PollAnswer
import org.matrix.android.sdk.api.session.room.model.message.PollCreationInfo
import org.matrix.android.sdk.api.session.room.model.message.PollQuestion
import org.matrix.android.sdk.api.session.room.model.message.PollType
import org.matrix.android.sdk.api.session.room.send.SendState

object PollFixture {

    val A_MESSAGE_INFORMATION_DATA = MessageInformationData(
            eventId = "eventId",
            senderId = "senderId",
            ageLocalTS = 0,
            avatarUrl = "",
            sendState = SendState.SENT,
            messageLayout = TimelineMessageLayout.Default(showAvatar = true, showDisplayName = true, showTimestamp = true),
            reactionsSummary = ReactionsSummaryData(),
            sentByMe = true,
    )

    val A_POLL_RESPONSE_DATA = PollResponseData(
            myVote = null,
            votes = emptyMap(),
    )

    val A_POLL_OPTION_IDS = listOf("5ef5f7b0-c9a1-49cf-a0b3-374729a43e76", "ec1a4db0-46d8-4d7a-9bb6-d80724715938", "3677ca8e-061b-40ab-bffe-b22e4e88fcad")

    val A_POLL_CONTENT = MessagePollContent(
            unstablePollCreationInfo = PollCreationInfo(
                    question = PollQuestion(
                            unstableQuestion = "What is your favourite coffee?"
                    ), kind = PollType.UNDISCLOSED_UNSTABLE, maxSelections = 1, answers = listOf(
                    PollAnswer(
                            id = A_POLL_OPTION_IDS[0], unstableAnswer = "Double Espresso"
                    ),
                    PollAnswer(
                            id = A_POLL_OPTION_IDS[1], unstableAnswer = "Macchiato"
                    ),
                    PollAnswer(
                            id = A_POLL_OPTION_IDS[2], unstableAnswer = "Iced Coffee"
                    ),
            )
            )
    )
}
