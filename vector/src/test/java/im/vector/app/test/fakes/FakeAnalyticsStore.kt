/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.test.fakes

import im.vector.app.features.analytics.store.AnalyticsStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking

class FakeAnalyticsStore {

    private val _consentFlow = MutableSharedFlow<Boolean>()
    private val _idFlow = MutableSharedFlow<String>()

    val instance = mockk<AnalyticsStore>(relaxed = true) {
        every { userConsentFlow } returns _consentFlow
        every { analyticsIdFlow } returns _idFlow
    }

    fun allowSettingAnalyticsIdToCallBackingFlow() {
        coEvery { instance.setAnalyticsId(any()) } answers {
            runBlocking { _idFlow.emit(firstArg()) }
        }
    }

    fun verifyConsentUpdated(updatedValue: Boolean) {
        coVerify { instance.setUserConsent(updatedValue) }
    }

    suspend fun givenUserContent(consent: Boolean) {
        _consentFlow.emit(consent)
    }

    fun verifyAnalyticsIdUpdated(updatedValue: String) {
        coVerify { instance.setAnalyticsId(updatedValue) }
    }

    suspend fun givenAnalyticsId(id: String) {
        _idFlow.emit(id)
    }
}
