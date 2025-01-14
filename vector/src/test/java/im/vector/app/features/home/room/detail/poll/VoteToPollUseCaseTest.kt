/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.poll

import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fixtures.RoomPollFixture
import io.mockk.every
import io.mockk.verify
import org.junit.Test
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.getTimelineEvent

private const val A_LOCAL_EVENT_ID = "\$local.event-id"
private const val AN_EVENT_ID = "event-id"
private const val A_ROOM_ID = "room-id"
private const val AN_EXISTING_OPTION_ID = "an-existing-option-id"
private const val AN_OPTION_ID = "option-id"
private const val AN_EVENT_TIMESTAMP = 123L

internal class VoteToPollUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val voteToPollUseCase = VoteToPollUseCase(fakeActiveSessionHolder.instance)

    @Test
    fun `given a local echo poll event when voting for an option then the vote is aborted`() {
        // Given
        val aPollEventId = A_LOCAL_EVENT_ID
        val aVoteId = AN_OPTION_ID
        givenAPollTimelineEvent(aPollEventId)

        // When
        voteToPollUseCase.execute(A_ROOM_ID, aPollEventId, aVoteId)

        // Then
        verify(exactly = 0) {
            fakeActiveSessionHolder.fakeSession
                    .getRoom(A_ROOM_ID)
                    ?.sendService()
                    ?.voteToPoll(aPollEventId, aVoteId)
        }
    }

    @Test
    fun `given a poll event when voting for a different option then the vote is sent`() {
        // Given
        val aPollEventId = AN_EVENT_ID
        val aVoteId = AN_OPTION_ID
        givenAPollTimelineEvent(aPollEventId)

        // When
        voteToPollUseCase.execute(A_ROOM_ID, aPollEventId, aVoteId)

        // Then
        verify(exactly = 1) {
            fakeActiveSessionHolder.fakeSession
                    .getRoom(A_ROOM_ID)
                    ?.sendService()
                    ?.voteToPoll(aPollEventId, aVoteId)
        }
    }

    @Test
    fun `given a poll event when voting for the same option then the vote is aborted`() {
        // Given
        val aPollEventId = AN_EVENT_ID
        val aVoteId = AN_EXISTING_OPTION_ID
        givenAPollTimelineEvent(aPollEventId)

        // When
        voteToPollUseCase.execute(A_ROOM_ID, aPollEventId, aVoteId)

        // Then
        verify(exactly = 0) {
            fakeActiveSessionHolder.fakeSession
                    .getRoom(A_ROOM_ID)
                    ?.sendService()
                    ?.voteToPoll(aPollEventId, aVoteId)
        }
    }

    private fun givenAPollTimelineEvent(eventId: String) {
        val pollCreationInfo = RoomPollFixture.givenPollCreationInfo("pollTitle")
        val messageContent = RoomPollFixture.givenAMessagePollContent(pollCreationInfo)
        val timelineEvent = RoomPollFixture.givenATimelineEvent(
                eventId,
                A_ROOM_ID,
                AN_EVENT_TIMESTAMP,
                messageContent
        )
        every {
            timelineEvent.annotations
                    ?.pollResponseSummary
                    ?.aggregatedContent
                    ?.myVote
        } returns AN_EXISTING_OPTION_ID

        every {
            fakeActiveSessionHolder.fakeSession
                    .getRoom(A_ROOM_ID)
                    ?.getTimelineEvent(eventId)
        } returns timelineEvent
    }
}
