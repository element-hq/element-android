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
