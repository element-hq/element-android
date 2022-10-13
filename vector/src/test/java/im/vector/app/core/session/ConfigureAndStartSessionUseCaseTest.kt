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

package im.vector.app.core.session

import im.vector.app.core.extensions.startSyncing
import im.vector.app.core.session.clientinfo.UpdateMatrixClientInfoUseCase
import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeVectorPreferences
import im.vector.app.test.fakes.FakeWebRtcCallManager
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.sync.FilterService

class ConfigureAndStartSessionUseCaseTest {

    private val fakeContext = FakeContext()
    private val fakeWebRtcCallManager = FakeWebRtcCallManager()
    private val fakeUpdateMatrixClientInfoUseCase = mockk<UpdateMatrixClientInfoUseCase>()
    private val fakeVectorPreferences = FakeVectorPreferences()

    private val configureAndStartSessionUseCase = ConfigureAndStartSessionUseCase(
            context = fakeContext.instance,
            webRtcCallManager = fakeWebRtcCallManager.instance,
            updateMatrixClientInfoUseCase = fakeUpdateMatrixClientInfoUseCase,
            vectorPreferences = fakeVectorPreferences.instance,
    )

    @Before
    fun setup() {
        mockkStatic("im.vector.app.core.extensions.SessionKt")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given start sync needed and client info recording enabled when execute then it should be configured properly`() = runTest {
        // Given
        val fakeSession = givenASession()
        fakeWebRtcCallManager.givenCheckForProtocolsSupportIfNeededSucceeds()
        coJustRun { fakeUpdateMatrixClientInfoUseCase.execute(any()) }
        fakeVectorPreferences.givenIsClientInfoRecordingEnabled(isEnabled = true)

        // When
        configureAndStartSessionUseCase.execute(fakeSession, startSyncing = true)

        // Then
        verify { fakeSession.startSyncing(fakeContext.instance) }
        fakeSession.fakeFilterService.verifySetFilter(FilterService.FilterPreset.ElementFilter)
        fakeSession.fakePushersService.verifyRefreshPushers()
        fakeWebRtcCallManager.verifyCheckForProtocolsSupportIfNeeded()
        coVerify { fakeUpdateMatrixClientInfoUseCase.execute(fakeSession) }
    }

    @Test
    fun `given start sync needed and client info recording disabled when execute then it should be configured properly`() = runTest {
        // Given
        val fakeSession = givenASession()
        fakeWebRtcCallManager.givenCheckForProtocolsSupportIfNeededSucceeds()
        coJustRun { fakeUpdateMatrixClientInfoUseCase.execute(any()) }
        fakeVectorPreferences.givenIsClientInfoRecordingEnabled(isEnabled = false)

        // When
        configureAndStartSessionUseCase.execute(fakeSession, startSyncing = true)

        // Then
        verify { fakeSession.startSyncing(fakeContext.instance) }
        fakeSession.fakeFilterService.verifySetFilter(FilterService.FilterPreset.ElementFilter)
        fakeSession.fakePushersService.verifyRefreshPushers()
        fakeWebRtcCallManager.verifyCheckForProtocolsSupportIfNeeded()
        coVerify(inverse = true) { fakeUpdateMatrixClientInfoUseCase.execute(fakeSession) }
    }

    @Test
    fun `given a session and no start sync needed when execute then it should be configured properly`() = runTest {
        // Given
        val fakeSession = givenASession()
        fakeWebRtcCallManager.givenCheckForProtocolsSupportIfNeededSucceeds()
        coJustRun { fakeUpdateMatrixClientInfoUseCase.execute(any()) }
        fakeVectorPreferences.givenIsClientInfoRecordingEnabled(isEnabled = true)

        // When
        configureAndStartSessionUseCase.execute(fakeSession, startSyncing = false)

        // Then
        verify(inverse = true) { fakeSession.startSyncing(fakeContext.instance) }
        fakeSession.fakeFilterService.verifySetFilter(FilterService.FilterPreset.ElementFilter)
        fakeSession.fakePushersService.verifyRefreshPushers()
        fakeWebRtcCallManager.verifyCheckForProtocolsSupportIfNeeded()
        coVerify { fakeUpdateMatrixClientInfoUseCase.execute(fakeSession) }
    }

    private fun givenASession(): FakeSession {
        val fakeSession = FakeSession()
        every { fakeSession.open() } just runs
        fakeSession.fakeFilterService.givenSetFilterSucceeds()
        every { fakeSession.startSyncing(any()) } just runs
        fakeSession.fakePushersService.givenRefreshPushersSucceeds()
        return fakeSession
    }
}
