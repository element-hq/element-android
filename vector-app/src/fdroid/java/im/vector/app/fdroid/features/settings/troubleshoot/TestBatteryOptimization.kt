/*
 * Copyright 2018-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.fdroid.features.settings.troubleshoot

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentActivity
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.isIgnoringBatteryOptimizations
import im.vector.app.core.utils.requestDisablingBatteryOptimization
import im.vector.app.features.settings.troubleshoot.TroubleshootTest
import javax.inject.Inject

class TestBatteryOptimization @Inject constructor(
        private val context: FragmentActivity,
        private val stringProvider: StringProvider
) : TroubleshootTest(R.string.settings_troubleshoot_test_battery_title) {

    override fun perform(activityResultLauncher: ActivityResultLauncher<Intent>) {
        if (context.isIgnoringBatteryOptimizations()) {
            description = stringProvider.getString(R.string.settings_troubleshoot_test_battery_success)
            status = TestStatus.SUCCESS
            quickFix = null
        } else {
            description = stringProvider.getString(R.string.settings_troubleshoot_test_battery_failed)
            quickFix = object : TroubleshootQuickFix(R.string.settings_troubleshoot_test_battery_quickfix) {
                override fun doFix() {
                    requestDisablingBatteryOptimization(context, activityResultLauncher)
                }
            }
            status = TestStatus.FAILED
        }
    }
}
