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

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentActivity
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.startNotificationSettingsIntent
import javax.inject.Inject

/**
 * Checks if notifications are enable in the system settings for this app.
 */
class TestSystemSettings @Inject constructor(private val context: FragmentActivity,
                                             private val stringProvider: StringProvider) :
    TroubleshootTest(R.string.settings_troubleshoot_test_system_settings_title) {

    override fun perform(activityResultLauncher: ActivityResultLauncher<Intent>) {
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            description = stringProvider.getString(R.string.settings_troubleshoot_test_system_settings_success)
            quickFix = null
            status = TestStatus.SUCCESS
        } else {
            description = stringProvider.getString(R.string.settings_troubleshoot_test_system_settings_failed)
            quickFix = object : TroubleshootQuickFix(R.string.open_settings) {
                override fun doFix() {
                    startNotificationSettingsIntent(context, activityResultLauncher)
                }
            }
            status = TestStatus.FAILED
        }
    }
}
