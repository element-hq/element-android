/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.gplay.features.settings.troubleshoot

import androidx.fragment.app.FragmentActivity
import com.google.firebase.messaging.FirebaseMessaging
import im.vector.app.core.pushers.FcmHelper
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.startAddGoogleAccountIntent
import im.vector.app.features.settings.troubleshoot.TroubleshootTest
import im.vector.lib.strings.CommonStrings
import timber.log.Timber
import javax.inject.Inject

/*
* Test that app can successfully retrieve a token via firebase
 */
class TestFirebaseToken @Inject constructor(
        private val context: FragmentActivity,
        private val stringProvider: StringProvider,
        private val fcmHelper: FcmHelper,
) : TroubleshootTest(CommonStrings.settings_troubleshoot_test_fcm_title) {

    override fun perform(testParameters: TestParameters) {
        status = TestStatus.RUNNING
        try {
            FirebaseMessaging.getInstance().token
                    .addOnCompleteListener(context) { task ->
                        if (!task.isSuccessful) {
                            // Can't find where this constant is (not documented -or deprecated in docs- and all obfuscated)
                            description = when (val errorMsg = task.exception?.localizedMessage ?: "Unknown") {
                                "SERVICE_NOT_AVAILABLE" -> {
                                    stringProvider.getString(CommonStrings.settings_troubleshoot_test_fcm_failed_service_not_available, errorMsg)
                                }
                                "TOO_MANY_REGISTRATIONS" -> {
                                    stringProvider.getString(CommonStrings.settings_troubleshoot_test_fcm_failed_too_many_registration, errorMsg)
                                }
                                "ACCOUNT_MISSING" -> {
                                    quickFix = object : TroubleshootQuickFix(CommonStrings.settings_troubleshoot_test_fcm_failed_account_missing_quick_fix) {
                                        override fun doFix() {
                                            startAddGoogleAccountIntent(context, testParameters.activityResultLauncher)
                                        }
                                    }
                                    stringProvider.getString(CommonStrings.settings_troubleshoot_test_fcm_failed_account_missing, errorMsg)
                                }
                                else -> {
                                    stringProvider.getString(CommonStrings.settings_troubleshoot_test_fcm_failed, errorMsg)
                                }
                            }
                            status = TestStatus.FAILED
                        } else {
                            task.result?.let { token ->
                                val tok = token.take(8) + "********************"
                                description = stringProvider.getString(CommonStrings.settings_troubleshoot_test_fcm_success, tok)
                                Timber.e("Retrieved FCM token success [$tok].")
                                // Ensure it is well store in our local storage
                                fcmHelper.storeFcmToken(token)
                            }
                            status = TestStatus.SUCCESS
                        }
                    }
        } catch (e: Throwable) {
            description = stringProvider.getString(CommonStrings.settings_troubleshoot_test_fcm_failed, e.localizedMessage)
            status = TestStatus.FAILED
        }
    }
}
