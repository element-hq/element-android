/*
 * Copyright 2018-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.settings.troubleshoot

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.startNotificationSettingsIntent
import im.vector.app.features.notifications.NotificationUtils
import javax.inject.Inject

/**
 * Checks if notifications can be displayed and clicked by the user.
 */
class TestNotification @Inject constructor(
        private val context: Context,
        private val notificationUtils: NotificationUtils,
        private val stringProvider: StringProvider
) :
        TroubleshootTest(R.string.settings_troubleshoot_test_notification_title) {

    override fun perform(activityResultLauncher: ActivityResultLauncher<Intent>) {
        // Display the notification right now
        notificationUtils.displayDiagnosticNotification()
        description = stringProvider.getString(R.string.settings_troubleshoot_test_notification_notice)

        quickFix = object : TroubleshootQuickFix(R.string.open_settings) {
            override fun doFix() {
                startNotificationSettingsIntent(context, activityResultLauncher)
            }
        }

        status = TestStatus.WAITING_FOR_USER
    }

    override fun onNotificationClicked() {
        description = stringProvider.getString(R.string.settings_troubleshoot_test_notification_notification_clicked)
        quickFix = null
        status = TestStatus.SUCCESS
    }
}
