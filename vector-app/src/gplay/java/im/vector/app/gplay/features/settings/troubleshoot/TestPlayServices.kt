/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.gplay.features.settings.troubleshoot

import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.settings.troubleshoot.TroubleshootTest
import im.vector.lib.strings.CommonStrings
import timber.log.Timber
import javax.inject.Inject

/*
* Check that the play services APK is available an up-to-date. If needed provide quick fix to install it.
 */
class TestPlayServices @Inject constructor(
        private val context: FragmentActivity,
        private val stringProvider: StringProvider
) :
        TroubleshootTest(CommonStrings.settings_troubleshoot_test_play_services_title) {

    override fun perform(testParameters: TestParameters) {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(context)
        if (resultCode == ConnectionResult.SUCCESS) {
            quickFix = null
            description = stringProvider.getString(CommonStrings.settings_troubleshoot_test_play_services_success)
            status = TestStatus.SUCCESS
        } else {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                quickFix = object : TroubleshootQuickFix(CommonStrings.settings_troubleshoot_test_play_services_quickfix) {
                    override fun doFix() {
                        apiAvailability.getErrorDialog(context, resultCode, 9000 /*hey does the magic number*/)?.show()
                    }
                }
                Timber.e("Play Services apk error $resultCode -> ${apiAvailability.getErrorString(resultCode)}.")
            }

            description = stringProvider.getString(CommonStrings.settings_troubleshoot_test_play_services_failed, apiAvailability.getErrorString(resultCode))
            status = TestStatus.FAILED
        }
    }
}
