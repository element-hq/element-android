/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.network

import org.amshove.kluent.shouldBeEqualTo
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runSessionTest
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants
import timber.log.Timber

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class ApiInterceptorTest : InstrumentedTest {

    @Test
    fun apiInterceptorTest() = runSessionTest(context()) { commonTestHelper ->
        val responses = mutableListOf<String>()

        val listener = object : ApiInterceptorListener {
            override fun onApiResponse(path: ApiPath, response: String) {
                Timber.w("onApiResponse($path): $response")
                responses.add(response)
            }
        }

        commonTestHelper.matrix.registerApiInterceptorListener(ApiPath.REGISTER, listener)

        val session = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(withInitialSync = true))

        commonTestHelper.signOutAndClose(session)

        commonTestHelper.matrix.unregisterApiInterceptorListener(ApiPath.REGISTER, listener)

        responses.size shouldBeEqualTo 2
    }
}
