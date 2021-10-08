/*
 * Copyright (c) 2021 New Vector Ltd
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

package org.matrix.android.sdk.internal.crypto.dehydration

import kotlinx.coroutines.DelicateCoroutinesApi
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@Suppress("SpellCheckingInspection")
class DehydrationKtTest: InstrumentedTest {

    private val commonTestHelper = CommonTestHelper(context())

    @DelicateCoroutinesApi
    @Test
    fun dehydrateDevice() {
        val session = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(withInitialSync = true))
//        val result = session.dehydrateDevice()
//        assert(result is DehydrationResult.Success)
//        val dehydratedDeviceId = (result as DehydrationResult.Success).deviceId
//        assert(dehydratedDeviceId.isNotEmpty())
//        assertEquals(dehydratedDeviceId, "asadsasdas")
//        session.close()
    }
}
