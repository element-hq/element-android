/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runSessionTest
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@Ignore("This test will be ignored until it is fixed")
class ChangePasswordTest : InstrumentedTest {

    companion object {
        private const val NEW_PASSWORD = "this is a new password"
    }

    @Test
    fun changePasswordTest() = runSessionTest(context()) { commonTestHelper ->
        val session = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(withInitialSync = false))

        // Change password
        commonTestHelper.runBlockingTest {
            session.accountService().changePassword(TestConstants.PASSWORD, NEW_PASSWORD)
        }

        // Try to login with the previous password, it will fail
        val throwable = commonTestHelper.logAccountWithError(session.myUserId, TestConstants.PASSWORD)
        throwable.isInvalidPassword().shouldBeTrue()

        // Try to login with the new password, should work
        commonTestHelper.logIntoAccount(session.myUserId, NEW_PASSWORD, SessionTestParams(withInitialSync = false))
    }
}
