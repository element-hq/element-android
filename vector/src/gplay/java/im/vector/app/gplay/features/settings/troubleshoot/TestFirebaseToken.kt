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
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.iid.FirebaseInstanceId
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.startAddGoogleAccountIntent
import im.vector.app.features.settings.troubleshoot.TroubleshootTest
import im.vector.app.push.fcm.FcmHelper
import timber.log.Timber
import javax.inject.Inject

/*
* Test that app can successfully retrieve a token via firebase
 */
class TestFirebaseToken @Inject constructor(private val context: AppCompatActivity,
                                            private val stringProvider: StringProvider) : TroubleshootTest(R.string.settings_troubleshoot_test_fcm_title) {

    override fun perform(activityResultLauncher: ActivityResultLauncher<Intent>) {
        status = TestStatus.RUNNING
        try {
            FirebaseInstanceId.getInstance().instanceId
                    .addOnCompleteListener(context) { task ->
                        if (!task.isSuccessful) {
                            val errorMsg = if (task.exception == null) "Unknown" else task.exception!!.localizedMessage
                            // Can't find where this constant is (not documented -or deprecated in docs- and all obfuscated)
                            if ("SERVICE_NOT_AVAILABLE".equals(errorMsg)) {
                                description = stringProvider.getString(R.string.settings_troubleshoot_test_fcm_failed_service_not_available, errorMsg)
                            } else if ("TOO_MANY_REGISTRATIONS".equals(errorMsg)) {
                                description = stringProvider.getString(R.string.settings_troubleshoot_test_fcm_failed_too_many_registration, errorMsg)
                            } else if ("ACCOUNT_MISSING".equals(errorMsg)) {
                                description = stringProvider.getString(R.string.settings_troubleshoot_test_fcm_failed_account_missing, errorMsg)
                                quickFix = object : TroubleshootQuickFix(R.string.settings_troubleshoot_test_fcm_failed_account_missing_quick_fix) {
                                    override fun doFix() {
                                        startAddGoogleAccountIntent(context, activityResultLauncher)
                                    }
                                }
                            } else {
                                description = stringProvider.getString(R.string.settings_troubleshoot_test_fcm_failed, errorMsg)
                            }
                            status = TestStatus.FAILED
                        } else {
                            task.result?.token?.let { token ->
                                val tok = token.substring(0, Math.min(8, token.length)) + "********************"
                                description = stringProvider.getString(R.string.settings_troubleshoot_test_fcm_success, tok)
                                Timber.e("Retrieved FCM token success [$tok].")
                                // Ensure it is well store in our local storage
                                FcmHelper.storeFcmToken(context, token)
                            }
                            status = TestStatus.SUCCESS
                        }
                    }
        } catch (e: Throwable) {
            description = stringProvider.getString(R.string.settings_troubleshoot_test_fcm_failed, e.localizedMessage)
            status = TestStatus.FAILED
        }
    }
}
