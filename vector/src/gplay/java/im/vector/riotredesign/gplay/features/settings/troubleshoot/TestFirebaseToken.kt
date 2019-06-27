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
package im.vector.riotredesign.gplay.features.settings.troubleshoot

import androidx.fragment.app.Fragment
import com.google.firebase.iid.FirebaseInstanceId
import im.vector.riotredesign.R
import im.vector.riotredesign.core.utils.startAddGoogleAccountIntent
import im.vector.riotredesign.features.settings.troubleshoot.NotificationTroubleshootTestManager
import im.vector.riotredesign.features.settings.troubleshoot.TroubleshootTest
import im.vector.riotredesign.push.fcm.FcmHelper
import timber.log.Timber

/*
* Test that app can successfully retrieve a token via firebase
 */
class TestFirebaseToken(val fragment: Fragment) : TroubleshootTest(R.string.settings_troubleshoot_test_fcm_title) {

    override fun perform() {
        status = TestStatus.RUNNING
        val activity = fragment.activity
        if (activity != null) {
            try {
                FirebaseInstanceId.getInstance().instanceId
                        .addOnCompleteListener(activity) { task ->
                            if (!task.isSuccessful) {
                                val errorMsg = if (task.exception == null) "Unknown" else task.exception!!.localizedMessage
                                //Can't find where this constant is (not documented -or deprecated in docs- and all obfuscated)
                                if ("SERVICE_NOT_AVAILABLE".equals(errorMsg)) {
                                    description = fragment.getString(R.string.settings_troubleshoot_test_fcm_failed_service_not_available, errorMsg)
                                } else if ("TOO_MANY_REGISTRATIONS".equals(errorMsg)) {
                                    description = fragment.getString(R.string.settings_troubleshoot_test_fcm_failed_too_many_registration, errorMsg)
                                } else if ("ACCOUNT_MISSING".equals(errorMsg)) {
                                    description = fragment.getString(R.string.settings_troubleshoot_test_fcm_failed_account_missing, errorMsg)
                                    quickFix = object : TroubleshootQuickFix(R.string.settings_troubleshoot_test_fcm_failed_account_missing_quick_fix) {
                                        override fun doFix() {
                                            startAddGoogleAccountIntent(fragment, NotificationTroubleshootTestManager.REQ_CODE_FIX)
                                        }
                                    }
                                } else {
                                    description = fragment.getString(R.string.settings_troubleshoot_test_fcm_failed, errorMsg)
                                }
                                status = TestStatus.FAILED
                            } else {
                                task.result?.token?.let {
                                    val tok = it.substring(0, Math.min(8, it.length)) + "********************"
                                    description = fragment.getString(R.string.settings_troubleshoot_test_fcm_success, tok)
                                    Timber.e("Retrieved FCM token success [$it].")
                                    FcmHelper.storeFcmToken(fragment.requireContext(),tok)
                                }
                                status = TestStatus.SUCCESS
                            }
                        }
            } catch (e: Throwable) {
                description = fragment.getString(R.string.settings_troubleshoot_test_fcm_failed, e.localizedMessage)
                status = TestStatus.FAILED
            }
        } else {
            status = TestStatus.FAILED
        }
    }

}