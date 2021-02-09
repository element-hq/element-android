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

package org.matrix.android.sdk.internal.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class CryptoServiceTest : InstrumentedTest {

    private val mTestHelper = CommonTestHelper(context())
    private val mCryptoTestHelper = CryptoTestHelper(mTestHelper)

    @Test
    fun ensure_outbound_session_happy_path() {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))
        val bobSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        // Initialize cross signing on both
        mCryptoTestHelper.initializeCrossSigning(aliceSession)
        mCryptoTestHelper.initializeCrossSigning(bobSession)

        val roomId = mCryptoTestHelper.createDM(aliceSession, bobSession)
        mCryptoTestHelper.verifySASCrossSign(aliceSession, bobSession, roomId)

        aliceSession.cryptoService().ensureOutboundSession(roomId)

        assertTrue(
                aliceSession
                        .cryptoService()
                        .getGossipingEvents()
                        .map { it.senderId }
                        .containsAll(
                                listOf(aliceSession.myUserId, bobSession.myUserId)
                        )
        )
    }
}
