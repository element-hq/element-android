/*
 * Copyright 2018 New Vector Ltd
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
package im.vector.app.features.settings.troubleshoot

import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentActivity
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.startNotificationSettingsIntent
import im.vector.app.features.home.NotificationPermissionManager
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
) : TroubleshootTest(R.string.settings_troubleshoot_test_system_settings_title) {

    override fun perform(testParameters: TestParameters) {
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            description = stringProvider.getString(R.string.settings_troubleshoot_test_system_settings_success)
            quickFix = null
            status = TestStatus.SUCCESS
        } else {
            if (sdkIntProvider.isAtLeast(Build.VERSION_CODES.TIRAMISU) && notificationPermissionManager.isPermissionGranted(context).not()) {
                // In this case, we can ask for user permission
                description = stringProvider.getString(R.string.settings_troubleshoot_test_system_settings_permission_failed)
                quickFix = object : TroubleshootQuickFix(R.string.grant_permission) {
                    override fun doFix() {
                        notificationPermissionManager.askPermission(testParameters.permissionResultLauncher)
                    }
                }
            } else {
                description = stringProvider.getString(R.string.settings_troubleshoot_test_system_settings_failed)
                quickFix = object : TroubleshootQuickFix(R.string.open_settings) {
                    override fun doFix() {
                        startNotificationSettingsIntent(context, testParameters.activityResultLauncher)
                    }
                }
            }
            status = TestStatus.FAILED
        }
    }
}
