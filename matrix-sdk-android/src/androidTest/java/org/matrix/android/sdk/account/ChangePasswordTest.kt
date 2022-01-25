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

import org.amshove.kluent.shouldBeTrue
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.failure.isInvalidPassword
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@Ignore("This test will be ignored until it is fixed")
class ChangePasswordTest : InstrumentedTest {

    private val commonTestHelper = CommonTestHelper(context())

    companion object {
        private const val NEW_PASSWORD = "this is a new password"
    }

    @Test
    fun changePasswordTest() {
        val session = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(withInitialSync = false))

        // Change password
        commonTestHelper.runBlockingTest {
            session.changePassword(TestConstants.PASSWORD, NEW_PASSWORD)
        }

        // Try to login with the previous password, it will fail
        val throwable = commonTestHelper.logAccountWithError(session.myUserId, TestConstants.PASSWORD)
        throwable.isInvalidPassword().shouldBeTrue()

        // Try to login with the new password, should work
        val session2 = commonTestHelper.logIntoAccount(session.myUserId, NEW_PASSWORD, SessionTestParams(withInitialSync = false))

        commonTestHelper.signOutAndClose(session)
        commonTestHelper.signOutAndClose(session2)
    }
}
