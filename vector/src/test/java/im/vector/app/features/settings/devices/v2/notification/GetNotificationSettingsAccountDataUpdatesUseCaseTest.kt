/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.notification

import im.vector.app.test.fakes.FakeFlowLiveDataConversions
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.givenAsFlow
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes
import org.matrix.android.sdk.api.session.events.model.toContent

class GetNotificationSettingsAccountDataUpdatesUseCaseTest {

    private val fakeFlowLiveDataConversions = FakeFlowLiveDataConversions()
    private val getNotificationSettingsAccountDataUpdatesUseCase = GetNotificationSettingsAccountDataUpdatesUseCase()

    @Before
    fun setUp() {
        fakeFlowLiveDataConversions.setup()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a device id when execute then retrieve the account data event corresponding to this id if any`() = runTest {
        // Given
        val aDeviceId = "device-id"
        val aSession = FakeSession()
        val expectedContent = LocalNotificationSettingsContent(isSilenced = true)
        aSession
                .accountDataService()
                .givenGetLiveUserAccountDataEventReturns(
                        type = UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS + aDeviceId,
                        content = expectedContent.toContent(),
                )
                .givenAsFlow()

        // When
        val result = getNotificationSettingsAccountDataUpdatesUseCase.execute(aSession, aDeviceId).firstOrNull()

        // Then
        result shouldBeEqualTo expectedContent
    }

    @Test
    fun `given a device id and no content for account data when execute then retrieve the account data event corresponding to this id if any`() = runTest {
        // Given
        val aDeviceId = "device-id"
        val aSession = FakeSession()
        aSession
                .accountDataService()
                .givenGetLiveUserAccountDataEventReturns(
                        type = UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS + aDeviceId,
                        content = null,
                )
                .givenAsFlow()

        // When
        val result = getNotificationSettingsAccountDataUpdatesUseCase.execute(aSession, aDeviceId).firstOrNull()

        // Then
        result shouldBeEqualTo null
    }

    @Test
    fun `given a device id and empty content for account data when execute then retrieve the account data event corresponding to this id if any`() = runTest {
        // Given
        val aDeviceId = "device-id"
        val aSession = FakeSession()
        val expectedContent = LocalNotificationSettingsContent(isSilenced = null)
        aSession
                .accountDataService()
                .givenGetLiveUserAccountDataEventReturns(
                        type = UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS + aDeviceId,
                        content = expectedContent.toContent(),
                )
                .givenAsFlow()

        // When
        val result = getNotificationSettingsAccountDataUpdatesUseCase.execute(aSession, aDeviceId).firstOrNull()

        // Then
        result shouldBeEqualTo expectedContent
    }
}
