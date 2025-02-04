/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.pushers

import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeFcmHelper
import im.vector.app.test.fakes.FakePushersManager
import im.vector.app.test.fakes.FakeUnifiedPushHelper
import im.vector.app.test.fixtures.PusherFixture
import org.junit.Test

class EnsureFcmTokenIsRetrievedUseCaseTest {

    private val fakeUnifiedPushHelper = FakeUnifiedPushHelper()
    private val fakeFcmHelper = FakeFcmHelper()
    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val ensureFcmTokenIsRetrievedUseCase = EnsureFcmTokenIsRetrievedUseCase(
            unifiedPushHelper = fakeUnifiedPushHelper.instance,
            fcmHelper = fakeFcmHelper.instance,
            activeSessionHolder = fakeActiveSessionHolder.instance,
    )

    @Test
    fun `given no registered pusher and distributor as embedded when execute then ensure the FCM token is retrieved with register pusher option`() {
        // Given
        val aPushersManager = FakePushersManager()
        fakeUnifiedPushHelper.givenIsEmbeddedDistributorReturns(true)
        fakeFcmHelper.givenEnsureFcmTokenIsRetrieved(aPushersManager.instance)
        val aSessionId = "aSessionId"
        fakeActiveSessionHolder.fakeSession.givenSessionId(aSessionId)
        val expectedPusher = PusherFixture.aPusher(deviceId = "")
        fakeActiveSessionHolder.fakeSession.fakePushersService.givenGetPushers(listOf(expectedPusher))

        // When
        ensureFcmTokenIsRetrievedUseCase.execute(aPushersManager.instance, registerPusher = true)

        // Then
        fakeFcmHelper.verifyEnsureFcmTokenIsRetrieved(aPushersManager.instance, registerPusher = true)
    }

    @Test
    fun `given a registered pusher and distributor as embedded when execute then ensure the FCM token is retrieved without register pusher option`() {
        // Given
        val aPushersManager = FakePushersManager()
        fakeUnifiedPushHelper.givenIsEmbeddedDistributorReturns(true)
        fakeFcmHelper.givenEnsureFcmTokenIsRetrieved(aPushersManager.instance)
        val aSessionId = "aSessionId"
        fakeActiveSessionHolder.fakeSession.givenSessionId(aSessionId)
        val expectedPusher = PusherFixture.aPusher(deviceId = aSessionId)
        fakeActiveSessionHolder.fakeSession.fakePushersService.givenGetPushers(listOf(expectedPusher))

        // When
        ensureFcmTokenIsRetrievedUseCase.execute(aPushersManager.instance, registerPusher = true)

        // Then
        fakeFcmHelper.verifyEnsureFcmTokenIsRetrieved(aPushersManager.instance, registerPusher = false)
    }

    @Test
    fun `given no registering asked and distributor as embedded when execute then ensure the FCM token is retrieved without register pusher option`() {
        // Given
        val aPushersManager = FakePushersManager()
        fakeUnifiedPushHelper.givenIsEmbeddedDistributorReturns(true)
        fakeFcmHelper.givenEnsureFcmTokenIsRetrieved(aPushersManager.instance)
        val aSessionId = "aSessionId"
        fakeActiveSessionHolder.fakeSession.givenSessionId(aSessionId)
        val expectedPusher = PusherFixture.aPusher(deviceId = aSessionId)
        fakeActiveSessionHolder.fakeSession.fakePushersService.givenGetPushers(listOf(expectedPusher))

        // When
        ensureFcmTokenIsRetrievedUseCase.execute(aPushersManager.instance, registerPusher = false)

        // Then
        fakeFcmHelper.verifyEnsureFcmTokenIsRetrieved(aPushersManager.instance, registerPusher = false)
    }

    @Test
    fun `given distributor as not embedded when execute then nothing is done`() {
        // Given
        val aPushersManager = FakePushersManager()
        fakeUnifiedPushHelper.givenIsEmbeddedDistributorReturns(false)

        // When
        ensureFcmTokenIsRetrievedUseCase.execute(aPushersManager.instance, registerPusher = true)

        // Then
        fakeFcmHelper.verifyEnsureFcmTokenIsRetrieved(aPushersManager.instance, registerPusher = true, inverse = true)
        fakeFcmHelper.verifyEnsureFcmTokenIsRetrieved(aPushersManager.instance, registerPusher = false, inverse = true)
    }
}
