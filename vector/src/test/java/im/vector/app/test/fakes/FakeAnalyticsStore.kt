/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
