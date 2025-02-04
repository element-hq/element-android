/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.fdroid.features.settings.troubleshoot

import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.ConnectivityManagerCompat
import androidx.fragment.app.FragmentActivity
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.settings.troubleshoot.TroubleshootTest
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

class TestBackgroundRestrictions @Inject constructor(
        private val context: FragmentActivity,
        private val stringProvider: StringProvider
) :
        TroubleshootTest(CommonStrings.settings_troubleshoot_test_bg_restricted_title) {

    override fun perform(testParameters: TestParameters) {
        context.getSystemService<ConnectivityManager>()!!.apply {
            // Checks if the device is on a metered network
            if (isActiveNetworkMetered) {
                // Checks user’s Data Saver settings.
                when (ConnectivityManagerCompat.getRestrictBackgroundStatus(this)) {
                    ConnectivityManagerCompat.RESTRICT_BACKGROUND_STATUS_ENABLED -> {
                        // Background data usage is blocked for this app. Wherever possible,
                        // the app should also use less data in the foreground.
                        description = stringProvider.getString(
                                CommonStrings.settings_troubleshoot_test_bg_restricted_failed,
                                "RESTRICT_BACKGROUND_STATUS_ENABLED"
                        )
                        status = TestStatus.FAILED
                        quickFix = null
                    }
                    ConnectivityManagerCompat.RESTRICT_BACKGROUND_STATUS_WHITELISTED -> {
                        // The app is whitelisted. Wherever possible,
                        // the app should use less data in the foreground and background.
                        description = stringProvider.getString(
                                CommonStrings.settings_troubleshoot_test_bg_restricted_success,
                                "RESTRICT_BACKGROUND_STATUS_WHITELISTED"
                        )
                        status = TestStatus.SUCCESS
                        quickFix = null
                    }
                    ConnectivityManagerCompat.RESTRICT_BACKGROUND_STATUS_DISABLED -> {
                        // Data Saver is disabled. Since the device is connected to a
                        // metered network, the app should use less data wherever possible.
                        description = stringProvider.getString(
                                CommonStrings.settings_troubleshoot_test_bg_restricted_success,
                                "RESTRICT_BACKGROUND_STATUS_DISABLED"
                        )
                        status = TestStatus.SUCCESS
                        quickFix = null
                    }
                }
            } else {
                // The device is not on a metered network.
                // Use data as required to perform syncs, downloads, and updates.
                description = stringProvider.getString(CommonStrings.settings_troubleshoot_test_bg_restricted_success, "")
                status = TestStatus.SUCCESS
                quickFix = null
            }
        }
    }
}
