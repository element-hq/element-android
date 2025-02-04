/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.settings.troubleshoot

import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentActivity
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.startNotificationSettingsIntent
import im.vector.app.features.home.NotificationPermissionManager
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider
import javax.inject.Inject

/**
 * Checks if notifications are enable in the system settings for this app.
 * On Android 13, it will check for the notification permission.
 */
class TestSystemSettings @Inject constructor(
        private val context: FragmentActivity,
        private val stringProvider: StringProvider,
        private val sdkIntProvider: BuildVersionSdkIntProvider,
        private val notificationPermissionManager: NotificationPermissionManager,
) : TroubleshootTest(CommonStrings.settings_troubleshoot_test_system_settings_title) {

    override fun perform(testParameters: TestParameters) {
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            description = stringProvider.getString(CommonStrings.settings_troubleshoot_test_system_settings_success)
            quickFix = null
            status = TestStatus.SUCCESS
        } else {
            if (sdkIntProvider.isAtLeast(Build.VERSION_CODES.TIRAMISU) && notificationPermissionManager.isPermissionGranted(context).not()) {
                // In this case, we can ask for user permission
                description = stringProvider.getString(CommonStrings.settings_troubleshoot_test_system_settings_permission_failed)
                quickFix = object : TroubleshootQuickFix(CommonStrings.grant_permission) {
                    override fun doFix() {
                        notificationPermissionManager.askPermission(testParameters.permissionResultLauncher)
                    }
                }
            } else {
                description = stringProvider.getString(CommonStrings.settings_troubleshoot_test_system_settings_failed)
                quickFix = object : TroubleshootQuickFix(CommonStrings.open_settings) {
                    override fun doFix() {
                        startNotificationSettingsIntent(context, testParameters.activityResultLauncher)
                    }
                }
            }
            status = TestStatus.FAILED
        }
    }
}
