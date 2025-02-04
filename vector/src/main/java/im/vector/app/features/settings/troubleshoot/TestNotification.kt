/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.settings.troubleshoot

import android.content.Context
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.startNotificationSettingsIntent
import im.vector.app.features.notifications.NotificationUtils
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

/**
 * Checks if notifications can be displayed and clicked by the user.
 */
class TestNotification @Inject constructor(
        private val context: Context,
        private val notificationUtils: NotificationUtils,
        private val stringProvider: StringProvider
) :
        TroubleshootTest(CommonStrings.settings_troubleshoot_test_notification_title) {

    override fun perform(testParameters: TestParameters) {
        // Display the notification right now
        notificationUtils.displayDiagnosticNotification()
        description = stringProvider.getString(CommonStrings.settings_troubleshoot_test_notification_notice)

        quickFix = object : TroubleshootQuickFix(CommonStrings.open_settings) {
            override fun doFix() {
                startNotificationSettingsIntent(context, testParameters.activityResultLauncher)
            }
        }

        status = TestStatus.WAITING_FOR_USER
    }

    override fun onNotificationClicked() {
        description = stringProvider.getString(CommonStrings.settings_troubleshoot_test_notification_notification_clicked)
        quickFix = null
        status = TestStatus.SUCCESS
    }
}
