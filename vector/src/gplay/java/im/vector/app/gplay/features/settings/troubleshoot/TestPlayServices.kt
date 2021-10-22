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
package im.vector.app.gplay.features.settings.troubleshoot

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.settings.troubleshoot.TroubleshootTest
import timber.log.Timber
import javax.inject.Inject

/*
* Check that the play services APK is available an up-to-date. If needed provide quick fix to install it.
 */
class TestPlayServices @Inject constructor(private val context: FragmentActivity,
                                           private val stringProvider: StringProvider) :
    TroubleshootTest(R.string.settings_troubleshoot_test_play_services_title) {

    override fun perform(activityResultLauncher: ActivityResultLauncher<Intent>) {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(context)
        if (resultCode == ConnectionResult.SUCCESS) {
            quickFix = null
            description = stringProvider.getString(R.string.settings_troubleshoot_test_play_services_success)
            status = TestStatus.SUCCESS
        } else {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                quickFix = object : TroubleshootQuickFix(R.string.settings_troubleshoot_test_play_services_quickfix) {
                    override fun doFix() {
                        apiAvailability.getErrorDialog(context, resultCode, 9000 /*hey does the magic number*/).show()
                    }
                }
                Timber.e("Play Services apk error $resultCode -> ${apiAvailability.getErrorString(resultCode)}.")
            }

            description = stringProvider.getString(R.string.settings_troubleshoot_test_play_services_failed, apiAvailability.getErrorString(resultCode))
            status = TestStatus.FAILED
        }
    }
}
