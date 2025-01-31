/*
 * Copyright 2018-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.fdroid.features.settings.troubleshoot

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.troubleshoot.TroubleshootTest
import javax.inject.Inject

/**
 * Test that the application is started on boot
 */
class TestAutoStartBoot @Inject constructor(
        private val vectorPreferences: VectorPreferences,
        private val stringProvider: StringProvider
) :
        TroubleshootTest(R.string.settings_troubleshoot_test_service_boot_title) {

    override fun perform(activityResultLauncher: ActivityResultLauncher<Intent>) {
        if (vectorPreferences.autoStartOnBoot()) {
            description = stringProvider.getString(R.string.settings_troubleshoot_test_service_boot_success)
            status = TestStatus.SUCCESS
            quickFix = null
        } else {
            description = stringProvider.getString(R.string.settings_troubleshoot_test_service_boot_failed)
            quickFix = object : TroubleshootQuickFix(R.string.settings_troubleshoot_test_service_boot_quickfix) {
                override fun doFix() {
                    vectorPreferences.setAutoStartOnBoot(true)
                    manager?.retry(activityResultLauncher)
                }
            }
            status = TestStatus.FAILED
        }
    }
}
