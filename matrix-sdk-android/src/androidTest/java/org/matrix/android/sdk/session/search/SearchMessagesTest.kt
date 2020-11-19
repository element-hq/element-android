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

package org.matrix.android.sdk.session.search

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.api.session.search.SearchResult
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.common.TestConstants
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class SearchMessagesTest : InstrumentedTest {

    private val MESSAGE = "Lorem ipsum dolor sit amet"

    private val commonTestHelper = CommonTestHelper(context())
    private val cryptoTestHelper = CryptoTestHelper(commonTestHelper)

    @Test
    fun sendTextMessageAndSearchPartOfItUsingSession() {
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(false)
        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId
        aliceSession.cryptoService().setWarnOnUnknownDevices(false)
        val roomFromAlicePOV = aliceSession.getRoom(aliceRoomId)!!
        val aliceTimeline = roomFromAlicePOV.createTimeline(null, TimelineSettings(10))
        aliceTimeline.start()

        commonTestHelper.sendTextMessage(
                roomFromAlicePOV,
                MESSAGE,
                2)

        run {
            var lock = CountDownLatch(1)

            val eventListener = commonTestHelper.createEventListener(lock) { snapshot ->
                snapshot.count { it.root.content.toModel<MessageContent>()?.body?.startsWith(MESSAGE).orFalse() } == 2
            }

            aliceTimeline.addListener(eventListener)
            commonTestHelper.await(lock)

            lock = CountDownLatch(1)
            val data = commonTestHelper.runBlockingTest {
                aliceSession
                        .searchService()
                        .search(
                                searchTerm = "lore",
                                limit = 10,
                                includeProfile = true,
                                afterLimit = 0,
                                beforeLimit = 10,
                                orderByRecent = true,
                                nextBatch = null,
                                roomId = aliceRoomId
                        )
            }
            assertTrue(data.results?.size == 2)
            assertTrue(
                    data.results
                            ?.all {
                                (it.event.content?.get("body") as? String)?.startsWith(MESSAGE).orFalse()
                            }.orFalse()
            )

            aliceTimeline.removeAllListeners()
            cryptoTestData.cleanUp(commonTestHelper)
        }

        aliceSession.startSync(true)
    }

    @Test
    fun sendTextMessageAndSearchPartOfItUsingRoom() {
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(false)
        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId
        aliceSession.cryptoService().setWarnOnUnknownDevices(false)
        val roomFromAlicePOV = aliceSession.getRoom(aliceRoomId)!!
        val aliceTimeline = roomFromAlicePOV.createTimeline(null, TimelineSettings(10))
        aliceTimeline.start()

        commonTestHelper.sendTextMessage(
                roomFromAlicePOV,
                MESSAGE,
                2)

        run {
            var lock = CountDownLatch(1)

            val eventListener = commonTestHelper.createEventListener(lock) { snapshot ->
                snapshot.count { it.root.content.toModel<MessageContent>()?.body?.startsWith(MESSAGE).orFalse() } == 2
            }

            aliceTimeline.addListener(eventListener)
            commonTestHelper.await(lock)

            lock = CountDownLatch(1)
            roomFromAlicePOV
                    .search(
                            searchTerm = "lore",
                            limit = 10,
                            includeProfile = true,
                            afterLimit = 0,
                            beforeLimit = 10,
                            orderByRecent = true,
                            nextBatch = null,
                            callback = object : MatrixCallback<SearchResult> {
                                override fun onSuccess(data: SearchResult) {
                                    super.onSuccess(data)
                                    assertTrue(data.results?.size == 2)
                                    assertTrue(
                                            data.results
                                                    ?.all {
                                                        (it.event.content?.get("body") as? String)?.startsWith(MESSAGE).orFalse()
                                                    }.orFalse()
                                    )
                                    lock.countDown()
                                }

                                override fun onFailure(failure: Throwable) {
                                    super.onFailure(failure)
                                    fail(failure.localizedMessage)
                                    lock.countDown()
                                }
                            }
                    )
            lock.await(TestConstants.timeOutMillis, TimeUnit.MILLISECONDS)

            aliceTimeline.removeAllListeners()
            cryptoTestData.cleanUp(commonTestHelper)
        }

        aliceSession.startSync(true)
    }
}
