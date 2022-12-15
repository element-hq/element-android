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

package im.vector.app.core.pushers

import im.vector.app.features.settings.BackgroundSyncMode
import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakePushersManager
import im.vector.app.test.fakes.FakeUnifiedPushHelper
import im.vector.app.test.fakes.FakeUnifiedPushStore
import im.vector.app.test.fakes.FakeVectorPreferences
import io.mockk.justRun
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verifyAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.unifiedpush.android.connector.UnifiedPush

class UnregisterUnifiedPushUseCaseTest {

    private val fakeContext = FakeContext()
    private val fakeVectorPreferences = FakeVectorPreferences()
    private val fakeUnifiedPushStore = FakeUnifiedPushStore()
    private val fakeUnifiedPushHelper = FakeUnifiedPushHelper()

    private val unregisterUnifiedPushUseCase = UnregisterUnifiedPushUseCase(
            context = fakeContext.instance,
            vectorPreferences = fakeVectorPreferences.instance,
            unifiedPushStore = fakeUnifiedPushStore.instance,
            unifiedPushHelper = fakeUnifiedPushHelper.instance,
    )

    @Before
    fun setup() {
        mockkStatic(UnifiedPush::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given pushersManager when execute then unregister and clean everything which is needed`() = runTest {
        // Given
        val aEndpoint = "endpoint"
        fakeUnifiedPushHelper.givenGetEndpointOrTokenReturns(aEndpoint)
        val aPushersManager = FakePushersManager()
        aPushersManager.givenUnregisterPusher(aEndpoint)
        justRun { UnifiedPush.unregisterApp(any()) }
        fakeVectorPreferences.givenSetFdroidSyncBackgroundMode(BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_REALTIME)
        fakeUnifiedPushStore.givenStorePushGateway(null)
        fakeUnifiedPushStore.givenStoreUpEndpoint(null)

        // When
        unregisterUnifiedPushUseCase.execute(aPushersManager.instance)

        // Then
        fakeVectorPreferences.verifySetFdroidSyncBackgroundMode(BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_REALTIME)
        aPushersManager.verifyUnregisterPusher(aEndpoint)
        verifyAll {
            UnifiedPush.unregisterApp(fakeContext.instance)
        }
        fakeUnifiedPushStore.verifyStorePushGateway(null)
        fakeUnifiedPushStore.verifyStoreUpEndpoint(null)
    }
}
