/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
