/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.features.home.room.detail.timeline.item.PollOptionViewState
import im.vector.app.features.home.room.detail.timeline.item.PollVoteSummaryData
import im.vector.app.features.poll.PollItemViewState
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fixtures.PollFixture.A_POLL_CONTENT
import im.vector.app.test.fixtures.PollFixture.A_POLL_OPTION_IDS
import im.vector.app.test.fixtures.PollFixture.A_POLL_RESPONSE_DATA
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.message.PollType

class PollItemViewStateFactoryTest {

    private val fakeStringProvider = FakeStringProvider()
    private val fakePollOptionViewStateFactory = mockk<PollOptionViewStateFactory>()

    private val pollItemViewStateFactory = PollItemViewStateFactory(
            stringProvider = fakeStringProvider.instance,
            pollOptionViewStateFactory = fakePollOptionViewStateFactory,
    )

    @Test
    fun `given a sending poll state then poll is not votable and option states are PollSending`() {
        // Given
        val optionViewStates = listOf(PollOptionViewState.PollSending(optionId = "", optionAnswer = ""))
        every { fakePollOptionViewStateFactory.createPollSendingOptions(A_POLL_CONTENT.getBestPollCreationInfo()) } returns optionViewStates

        // When
        val pollViewState = pollItemViewStateFactory.create(
                pollContent = A_POLL_CONTENT,
                pollResponseData = null,
                isSent = false,
        )

        // Then
        pollViewState shouldBeEqualTo PollItemViewState(
                question = A_POLL_CONTENT.getBestPollCreationInfo()?.question?.getBestQuestion() ?: "",
                votesStatus = fakeStringProvider.instance.getString(CommonStrings.poll_no_votes_cast),
                canVote = false,
                optionViewStates = optionViewStates,
        )
        verify { fakePollOptionViewStateFactory.createPollSendingOptions(A_POLL_CONTENT.getBestPollCreationInfo()) }
    }

    @Test
    fun `given a sent poll state when poll is closed then poll is not votable and option states are Ended`() {
        // Given
        val closedPollSummary = A_POLL_RESPONSE_DATA.copy(isClosed = true)
        val optionViewStates = listOf(
                PollOptionViewState.PollEnded(
                        optionId = "", optionAnswer = "", voteCount = 0, votePercentage = 0.0, isWinner = false
                )
        )
        every {
            fakePollOptionViewStateFactory.createPollEndedOptions(
                    A_POLL_CONTENT.getBestPollCreationInfo(),
                    closedPollSummary,
            )
        } returns optionViewStates

        // When
        val pollViewState = pollItemViewStateFactory.create(
                pollContent = A_POLL_CONTENT,
                pollResponseData = closedPollSummary,
                isSent = true,
        )

        // Then
        pollViewState shouldBeEqualTo PollItemViewState(
                question = A_POLL_CONTENT.getBestPollCreationInfo()?.question?.getBestQuestion() ?: "",
                votesStatus = fakeStringProvider.instance.getQuantityString(CommonPlurals.poll_total_vote_count_after_ended, 0, 0),
                canVote = false,
                optionViewStates = optionViewStates,
        )
        verify {
            fakePollOptionViewStateFactory.createPollEndedOptions(
                    A_POLL_CONTENT.getBestPollCreationInfo(),
                    closedPollSummary,
            )
        }
    }

    @Test
    fun `given a sent poll state with some decryption error when poll is closed then warning message is displayed`() {
        // Given
        val closedPollSummary = A_POLL_RESPONSE_DATA.copy(isClosed = true, hasEncryptedRelatedEvents = true)
        val optionViewStates = listOf(
                PollOptionViewState.PollEnded(
                        optionId = "", optionAnswer = "", voteCount = 0, votePercentage = 0.0, isWinner = false
                )
        )
        every {
            fakePollOptionViewStateFactory.createPollEndedOptions(
                    A_POLL_CONTENT.getBestPollCreationInfo(),
                    closedPollSummary,
            )
        } returns optionViewStates

        // When
        val pollViewState = pollItemViewStateFactory.create(
                pollContent = A_POLL_CONTENT,
                pollResponseData = closedPollSummary,
                isSent = true,
        )

        // Then
        pollViewState.votesStatus shouldBeEqualTo fakeStringProvider.instance.getString(CommonStrings.unable_to_decrypt_some_events_in_poll)
    }

    @Test
    fun `given a sent poll when undisclosed poll type is selected then poll is votable and option states are PollUndisclosed`() {
        // Given
        val pollResponseData = A_POLL_RESPONSE_DATA
        val optionViewStates = listOf(
                PollOptionViewState.PollUndisclosed(
                        optionId = "",
                        optionAnswer = "",
                        isSelected = false,
                )
        )
        every {
            fakePollOptionViewStateFactory.createPollUndisclosedOptions(
                    A_POLL_CONTENT.getBestPollCreationInfo(),
                    pollResponseData,
            )
        } returns optionViewStates

        // When
        val pollViewState = pollItemViewStateFactory.create(
                pollContent = A_POLL_CONTENT,
                pollResponseData = pollResponseData,
                isSent = true,
        )

        // Then
        pollViewState shouldBeEqualTo PollItemViewState(
                question = A_POLL_CONTENT.getBestPollCreationInfo()?.question?.getBestQuestion() ?: "",
                votesStatus = fakeStringProvider.instance.getString(CommonStrings.poll_undisclosed_not_ended),
                canVote = true,
                optionViewStates = optionViewStates,
        )
        verify {
            fakePollOptionViewStateFactory.createPollUndisclosedOptions(
                    A_POLL_CONTENT.getBestPollCreationInfo(),
                    A_POLL_RESPONSE_DATA,
            )
        }
    }

    @Test
    fun `given a sent poll when my vote exists then poll is still votable and options states are PollVoted`() {
        // Given
        val votedPollData = A_POLL_RESPONSE_DATA.copy(
                totalVotes = 1, myVote = A_POLL_OPTION_IDS[0], votes = mapOf(A_POLL_OPTION_IDS[0] to PollVoteSummaryData(total = 1, percentage = 1.0))
        )
        val disclosedPollContent = A_POLL_CONTENT.copy(
                unstablePollCreationInfo = A_POLL_CONTENT.getBestPollCreationInfo()?.copy(
                        kind = PollType.DISCLOSED_UNSTABLE
                ),
        )
        val optionViewStates = listOf(
                PollOptionViewState.PollVoted(
                        optionId = "",
                        optionAnswer = "",
                        voteCount = 0,
                        votePercentage = 0.0,
                        isSelected = false,
                )
        )
        every {
            fakePollOptionViewStateFactory.createPollVotedOptions(
                    disclosedPollContent.getBestPollCreationInfo(),
                    votedPollData,
            )
        } returns optionViewStates

        // When
        val pollViewState = pollItemViewStateFactory.create(
                pollContent = disclosedPollContent,
                pollResponseData = votedPollData,
                isSent = true,
        )

        // Then
        pollViewState shouldBeEqualTo PollItemViewState(
                question = A_POLL_CONTENT.getBestPollCreationInfo()?.question?.getBestQuestion() ?: "",
                votesStatus = fakeStringProvider.instance.getQuantityString(CommonPlurals.poll_total_vote_count_before_ended_and_voted, 1, 1),
                canVote = true,
                optionViewStates = optionViewStates,
        )
        verify {
            fakePollOptionViewStateFactory.createPollVotedOptions(
                    disclosedPollContent.getBestPollCreationInfo(),
                    votedPollData,
            )
        }
    }

    @Test
    fun `given a sent poll with decryption failure when my vote exists then a warning message is displayed`() {
        // Given
        val votedPollData = A_POLL_RESPONSE_DATA.copy(
                totalVotes = 1,
                myVote = A_POLL_OPTION_IDS[0],
                votes = mapOf(A_POLL_OPTION_IDS[0] to PollVoteSummaryData(total = 1, percentage = 1.0)),
                hasEncryptedRelatedEvents = true,
        )
        val disclosedPollContent = A_POLL_CONTENT.copy(
                unstablePollCreationInfo = A_POLL_CONTENT.getBestPollCreationInfo()?.copy(
                        kind = PollType.DISCLOSED_UNSTABLE
                ),
        )
        val optionViewStates = listOf(
                PollOptionViewState.PollVoted(
                        optionId = "",
                        optionAnswer = "",
                        voteCount = 0,
                        votePercentage = 0.0,
                        isSelected = false,
                )
        )
        every {
            fakePollOptionViewStateFactory.createPollVotedOptions(
                    disclosedPollContent.getBestPollCreationInfo(),
                    votedPollData,
            )
        } returns optionViewStates

        // When
        val pollViewState = pollItemViewStateFactory.create(
                pollContent = disclosedPollContent,
                pollResponseData = votedPollData,
                isSent = true,
        )

        // Then
        pollViewState.votesStatus shouldBeEqualTo fakeStringProvider.instance.getString(CommonStrings.unable_to_decrypt_some_events_in_poll)
    }

    @Test
    fun `given a sent poll when poll type is disclosed then poll is votable and option view states are PollReady`() {
        // Given
        val pollResponseData = A_POLL_RESPONSE_DATA
        val disclosedPollContent = A_POLL_CONTENT.copy(
                unstablePollCreationInfo = A_POLL_CONTENT.getBestPollCreationInfo()?.copy(
                        kind = PollType.DISCLOSED_UNSTABLE
                )
        )
        val optionViewStates = listOf(
                PollOptionViewState.PollReady(
                        optionId = "",
                        optionAnswer = "",
                )
        )
        every {
            fakePollOptionViewStateFactory.createPollReadyOptions(
                    disclosedPollContent.getBestPollCreationInfo(),
            )
        } returns optionViewStates

        // When
        val pollViewState = pollItemViewStateFactory.create(
                pollContent = disclosedPollContent,
                pollResponseData = pollResponseData,
                isSent = true,
        )

        // Then
        pollViewState shouldBeEqualTo PollItemViewState(
                question = A_POLL_CONTENT.getBestPollCreationInfo()?.question?.getBestQuestion() ?: "",
                votesStatus = fakeStringProvider.instance.getString(CommonStrings.poll_no_votes_cast),
                canVote = true,
                optionViewStates = optionViewStates,
        )
        verify {
            fakePollOptionViewStateFactory.createPollReadyOptions(
                    disclosedPollContent.getBestPollCreationInfo(),
            )
        }
    }
}
