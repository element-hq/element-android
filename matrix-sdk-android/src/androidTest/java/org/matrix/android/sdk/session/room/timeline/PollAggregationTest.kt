/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.session.room.timeline

import org.amshove.kluent.fail
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainAll
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.PollResponseAggregatedSummary
import org.matrix.android.sdk.api.session.room.model.PollSummaryContent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.PollType
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import java.util.concurrent.CountDownLatch

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class PollAggregationTest : InstrumentedTest {

    @Test
    fun testAllPollUseCases() {
        val commonTestHelper = CommonTestHelper(context())
        val cryptoTestHelper = CryptoTestHelper(commonTestHelper)
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(false)

        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId
        val roomFromAlicePOV = aliceSession.getRoom(aliceRoomId)!!

        val roomFromBobPOV = cryptoTestData.secondSession!!.getRoom(cryptoTestData.roomId)!!
        // Bob creates a poll
        roomFromBobPOV.sendPoll(PollType.DISCLOSED, pollQuestion, pollOptions)

        aliceSession.startSync(true)
        val aliceTimeline = roomFromAlicePOV.createTimeline(null, TimelineSettings(30))
        aliceTimeline.start()

        val TOTAL_TEST_COUNT = 5
        val lock = CountDownLatch(TOTAL_TEST_COUNT)

        val aliceEventsListener = object : Timeline.Listener {
            override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
                snapshot.firstOrNull { it.root.getClearType() == EventType.POLL_START }?.let { pollEvent ->
                    val pollEventId = pollEvent.eventId
                    val pollContent = pollEvent.root.content?.toModel<MessagePollContent>()
                    val pollSummary = pollEvent.annotations?.pollResponseSummary

                    if (pollContent == null) {
                        fail("Poll content is null")
                        return
                    }

                    when (lock.count.toInt()) {
                        TOTAL_TEST_COUNT     -> {
                            // Poll has just been created.
                            testInitialPollConditions(pollContent, pollSummary)
                            lock.countDown()
                            roomFromBobPOV.voteToPoll(pollEventId, pollContent.pollCreationInfo?.answers?.firstOrNull()?.id ?: "")
                        }
                        TOTAL_TEST_COUNT - 1 -> {
                            testBobVotesOption1(pollContent, pollSummary)
                            lock.countDown()
                            roomFromBobPOV.voteToPoll(pollEventId, pollContent.pollCreationInfo?.answers?.get(1)?.id ?: "")
                        }
                        TOTAL_TEST_COUNT - 2 -> {
                            testBobChangesVoteToOption2(pollContent, pollSummary)
                            lock.countDown()
                            roomFromAlicePOV.voteToPoll(pollEventId, pollContent.pollCreationInfo?.answers?.get(1)?.id ?: "")
                        }
                        TOTAL_TEST_COUNT - 3 -> {
                            testAliceAndBobVoteToOption2(pollContent, pollSummary)
                            lock.countDown()
                            roomFromAlicePOV.voteToPoll(pollEventId, pollContent.pollCreationInfo?.answers?.firstOrNull()?.id ?: "")
                        }
                        TOTAL_TEST_COUNT - 4 -> {
                            testAliceVotesOption1AndBobVotesOption2(pollContent, pollSummary)
                            lock.countDown()
                        }
                        else                 -> {
                            fail("Lock count ${lock.count} didn't handled.")
                        }
                    }
                }
            }
        }

        aliceTimeline.addListener(aliceEventsListener)

        commonTestHelper.await(lock)

        aliceTimeline.removeAllListeners()

        aliceSession.stopSync()
        aliceTimeline.dispose()
        cryptoTestData.cleanUp(commonTestHelper)
    }

    private fun testInitialPollConditions(pollContent: MessagePollContent, pollSummary: PollResponseAggregatedSummary?) {
        // No votes yet, poll summary should be null
        pollSummary shouldBe null
        // Question should be the same as intended
        pollContent.pollCreationInfo?.question?.question shouldBeEqualTo pollQuestion
        // Options should be the same as intended
        pollContent.pollCreationInfo?.answers?.let { answers ->
            answers.size shouldBeEqualTo pollOptions.size
            answers.map { it.answer } shouldContainAll pollOptions
        }
    }

    private fun testBobVotesOption1(pollContent: MessagePollContent, pollSummary: PollResponseAggregatedSummary?) {
        if (pollSummary == null) {
            fail("Poll summary shouldn't be null when someone votes")
            return
        }
        val answerId = pollContent.pollCreationInfo?.answers?.first()?.id
        // Check if the intended vote is in poll summary
        pollSummary.aggregatedContent?.let { aggregatedContent ->
            assertTotalVotesCount(aggregatedContent, 1)
            aggregatedContent.votes?.first()?.option shouldBeEqualTo answerId
            aggregatedContent.votesSummary?.get(answerId)?.total shouldBeEqualTo 1
            aggregatedContent.votesSummary?.get(answerId)?.percentage shouldBeEqualTo 1.0
        } ?: run { fail("Aggregated poll content shouldn't be null after someone votes") }
    }

    private fun testBobChangesVoteToOption2(pollContent: MessagePollContent, pollSummary: PollResponseAggregatedSummary?) {
        if (pollSummary == null) {
            fail("Poll summary shouldn't be null when someone votes")
            return
        }
        val answerId = pollContent.pollCreationInfo?.answers?.get(1)?.id
        // Check if the intended vote is in poll summary
        pollSummary.aggregatedContent?.let { aggregatedContent ->
            assertTotalVotesCount(aggregatedContent, 1)
            aggregatedContent.votes?.first()?.option shouldBeEqualTo answerId
            aggregatedContent.votesSummary?.get(answerId)?.total shouldBeEqualTo 1
            aggregatedContent.votesSummary?.get(answerId)?.percentage shouldBeEqualTo 1.0
        } ?: run { fail("Aggregated poll content shouldn't be null after someone votes") }
    }

    private fun testAliceAndBobVoteToOption2(pollContent: MessagePollContent, pollSummary: PollResponseAggregatedSummary?) {
        if (pollSummary == null) {
            fail("Poll summary shouldn't be null when someone votes")
            return
        }
        val answerId = pollContent.pollCreationInfo?.answers?.get(1)?.id
        // Check if the intended votes is in poll summary
        pollSummary.aggregatedContent?.let { aggregatedContent ->
            assertTotalVotesCount(aggregatedContent, 2)
            aggregatedContent.votes?.first()?.option shouldBeEqualTo answerId
            aggregatedContent.votes?.get(1)?.option shouldBeEqualTo answerId
            aggregatedContent.votesSummary?.get(answerId)?.total shouldBeEqualTo 2
            aggregatedContent.votesSummary?.get(answerId)?.percentage shouldBeEqualTo 1.0
        } ?: run { fail("Aggregated poll content shouldn't be null after someone votes") }
    }

    private fun testAliceVotesOption1AndBobVotesOption2(pollContent: MessagePollContent, pollSummary: PollResponseAggregatedSummary?) {
        if (pollSummary == null) {
            fail("Poll summary shouldn't be null when someone votes")
            return
        }
        val firstAnswerId = pollContent.pollCreationInfo?.answers?.firstOrNull()?.id
        val secondAnswerId = pollContent.pollCreationInfo?.answers?.get(1)?.id
        // Check if the intended votes is in poll summary
        pollSummary.aggregatedContent?.let { aggregatedContent ->
            assertTotalVotesCount(aggregatedContent, 2)
            aggregatedContent.votes!!.map { it.option } shouldContain firstAnswerId
            aggregatedContent.votes!!.map { it.option } shouldBeEqualTo secondAnswerId
            aggregatedContent.votesSummary?.get(firstAnswerId)?.total shouldBeEqualTo 1
            aggregatedContent.votesSummary?.get(secondAnswerId)?.total shouldBeEqualTo 1
            aggregatedContent.votesSummary?.get(firstAnswerId)?.percentage shouldBeEqualTo 0.5
            aggregatedContent.votesSummary?.get(secondAnswerId)?.percentage shouldBeEqualTo 0.5
        } ?: run { fail("Aggregated poll content shouldn't be null after someone votes") }
    }

    private fun assertTotalVotesCount(aggregatedContent: PollSummaryContent, expectedVoteCount: Int) {
        aggregatedContent.totalVotes shouldBeEqualTo expectedVoteCount
        aggregatedContent.votes?.size shouldBeEqualTo expectedVoteCount
    }

    companion object {
        const val pollQuestion = "Do you like creating polls?"
        val pollOptions = listOf("Yes", "Absolutely", "As long as tests pass")
    }
}
