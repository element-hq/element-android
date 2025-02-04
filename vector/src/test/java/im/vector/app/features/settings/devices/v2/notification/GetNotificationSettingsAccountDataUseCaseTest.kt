/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.notification

import im.vector.app.test.fakes.FakeSession
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes
import org.matrix.android.sdk.api.session.events.model.toContent

class GetNotificationSettingsAccountDataUseCaseTest {

    private val getNotificationSettingsAccountDataUseCase = GetNotificationSettingsAccountDataUseCase()

    @Test
    fun `given a device id when execute then retrieve the account data event corresponding to this id if any`() {
        // Given
        val aDeviceId = "device-id"
        val aSession = FakeSession()
        val expectedContent = LocalNotificationSettingsContent(isSilenced = true)
        aSession
                .accountDataService()
                .givenGetUserAccountDataEventReturns(
                        type = UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS + aDeviceId,
                        content = expectedContent.toContent(),
                )

        // When
        val result = getNotificationSettingsAccountDataUseCase.execute(aSession, aDeviceId)

        // Then
        result shouldBeEqualTo expectedContent
    }

    @Test
    fun `given a device id and empty content when execute then retrieve the account data event corresponding to this id if any`() {
        // Given
        val aDeviceId = "device-id"
        val aSession = FakeSession()
        val expectedContent = LocalNotificationSettingsContent(isSilenced = null)
        aSession
                .accountDataService()
                .givenGetUserAccountDataEventReturns(
                        type = UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS + aDeviceId,
                        content = expectedContent.toContent(),
                )

        // When
        val result = getNotificationSettingsAccountDataUseCase.execute(aSession, aDeviceId)

        // Then
        result shouldBeEqualTo expectedContent
    }
}
