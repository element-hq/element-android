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

package org.matrix.android.sdk.account

import androidx.test.filters.LargeTest
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runSuspendingCryptoTest
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runSuspendingSessionTest
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class AccountCreationTest : InstrumentedTest {

    @Test
    fun createAccountTest() = runSuspendingSessionTest(context()) { commonTestHelper ->
        commonTestHelper.createAccountSuspending(TestConstants.USER_ALICE, SessionTestParams(withInitialSync = true))
    }

    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun createAccountAndLoginAgainTest() = runSuspendingSessionTest(context()) { commonTestHelper ->
        val session = commonTestHelper.createAccountSuspending(TestConstants.USER_ALICE, SessionTestParams(withInitialSync = true))

        // Log again to the same account
        commonTestHelper.logIntoAccount(session.myUserId, SessionTestParams(withInitialSync = true))
    }

    @Test
    fun simpleE2eTest() = runSuspendingCryptoTest(context()) { cryptoTestHelper, _ ->
        cryptoTestHelper.doE2ETestWithAliceInARoom()
    }
}
