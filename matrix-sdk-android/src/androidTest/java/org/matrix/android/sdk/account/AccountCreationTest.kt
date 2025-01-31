/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runCryptoTest
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runSessionTest
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class AccountCreationTest : InstrumentedTest {

    @Test
    fun createAccountTest() = runSessionTest(context()) { commonTestHelper ->
        commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(withInitialSync = true))
    }

    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun createAccountAndLoginAgainTest() = runSessionTest(context()) { commonTestHelper ->
        val session = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(withInitialSync = true))

        // Log again to the same account
        commonTestHelper.logIntoAccount(session.myUserId, SessionTestParams(withInitialSync = true))
    }

    @Test
    fun simpleE2eTest() = runCryptoTest(context()) { cryptoTestHelper, _ ->
        cryptoTestHelper.doE2ETestWithAliceInARoom()
    }
}
