/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
