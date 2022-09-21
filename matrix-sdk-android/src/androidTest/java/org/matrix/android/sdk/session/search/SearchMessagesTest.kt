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
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.search.SearchResult
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runSuspendingCryptoTest
import org.matrix.android.sdk.common.CryptoTestData

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class SearchMessagesTest : InstrumentedTest {

    companion object {
        private const val MESSAGE = "Lorem ipsum dolor sit amet"
    }

    @Test
    fun sendTextMessageAndSearchPartOfItUsingSession() {
        doTest { cryptoTestData ->
            cryptoTestData.firstSession
                    .searchService()
                    .search(
                            searchTerm = "lore",
                            limit = 10,
                            includeProfile = true,
                            afterLimit = 0,
                            beforeLimit = 10,
                            orderByRecent = true,
                            nextBatch = null,
                            roomId = cryptoTestData.roomId
                    )
        }
    }

    @Test
    fun sendTextMessageAndSearchPartOfItUsingRoom() {
        doTest { cryptoTestData ->
            cryptoTestData.firstSession
                    .searchService()
                    .search(
                            searchTerm = "lore",
                            roomId = cryptoTestData.roomId,
                            limit = 10,
                            includeProfile = true,
                            afterLimit = 0,
                            beforeLimit = 10,
                            orderByRecent = true,
                            nextBatch = null
                    )
        }
    }

    private fun doTest(block: suspend (CryptoTestData) -> SearchResult) = runSuspendingCryptoTest(context()) { cryptoTestHelper, commonTestHelper ->
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceInARoom(false)
        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId
        val roomFromAlicePOV = aliceSession.getRoom(aliceRoomId)!!

        commonTestHelper.sendTextMessageSuspending(
                roomFromAlicePOV,
                MESSAGE,
                2
        )

        val data = block.invoke(cryptoTestData)

        assertTrue(data.results?.size == 2)
        assertTrue(
                data.results
                        ?.all {
                            (it.event.content?.get("body") as? String)?.startsWith(MESSAGE).orFalse()
                        }.orFalse()
        )
    }
}
