/*
 * Copyright 2018-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.settings.troubleshoot

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.settings.VectorPreferences
import javax.inject.Inject

/**
 * Checks if notifications are enable in the system settings for this app.
 */
class TestDeviceSettings @Inject constructor(
        private val vectorPreferences: VectorPreferences,
        private val stringProvider: StringProvider
) :
        TroubleshootTest(R.string.settings_troubleshoot_test_device_settings_title) {

    override fun perform(activityResultLauncher: ActivityResultLauncher<Intent>) {
        if (vectorPreferences.areNotificationEnabledForDevice()) {
            description = stringProvider.getString(R.string.settings_troubleshoot_test_device_settings_success)
            quickFix = null
            status = TestStatus.SUCCESS
        } else {
            quickFix = object : TroubleshootQuickFix(R.string.settings_troubleshoot_test_device_settings_quickfix) {
                override fun doFix() {
                    vectorPreferences.setNotificationEnabledForDevice(true)
                    manager?.retry(activityResultLauncher)
                }
            }
            description = stringProvider.getString(R.string.settings_troubleshoot_test_device_settings_failed)
            status = TestStatus.FAILED
        }
    }
}
