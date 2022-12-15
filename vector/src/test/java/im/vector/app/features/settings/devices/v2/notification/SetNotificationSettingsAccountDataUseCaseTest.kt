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
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes
import org.matrix.android.sdk.api.session.events.model.toContent

class SetNotificationSettingsAccountDataUseCaseTest {

    private val setNotificationSettingsAccountDataUseCase = SetNotificationSettingsAccountDataUseCase()

    @Test
    fun `given a content when execute then update local notification settings with this content`() = runTest {
        // Given
        val sessionId = "a_session_id"
        val localNotificationSettingsContent = LocalNotificationSettingsContent(isSilenced = true)
        val fakeSession = FakeSession()
        fakeSession.accountDataService().givenUpdateUserAccountDataEventSucceeds()

        // When
        setNotificationSettingsAccountDataUseCase.execute(fakeSession, sessionId, localNotificationSettingsContent)

        // Then
        fakeSession.accountDataService().verifyUpdateUserAccountDataEventSucceeds(
                UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS + sessionId,
                localNotificationSettingsContent.toContent(),
        )
    }
}
