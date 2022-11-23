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
