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

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.pushers.FcmHelper
import im.vector.app.core.pushers.PushersManager
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.settings.troubleshoot.TroubleshootTest
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.pushers.PusherState
import javax.inject.Inject

/**
 * Force registration of the token to HomeServer
 */
class TestTokenRegistration @Inject constructor(
        private val context: FragmentActivity,
        private val stringProvider: StringProvider,
        private val pushersManager: PushersManager,
        private val activeSessionHolder: ActiveSessionHolder,
        private val fcmHelper: FcmHelper,
) :
        TroubleshootTest(CommonStrings.settings_troubleshoot_test_token_registration_title) {

    override fun perform(testParameters: TestParameters) {
        // Check if we have a registered pusher for this token
        val fcmToken = fcmHelper.getFcmToken() ?: run {
            status = TestStatus.FAILED
            return
        }
        val session = activeSessionHolder.getSafeActiveSession() ?: run {
            status = TestStatus.FAILED
            return
        }
        val pushers = session.pushersService().getPushers().filter {
            it.pushKey == fcmToken && it.state == PusherState.REGISTERED
        }
        if (pushers.isEmpty()) {
            description = stringProvider.getString(
                    CommonStrings.settings_troubleshoot_test_token_registration_failed,
                    stringProvider.getString(CommonStrings.sas_error_unknown)
            )
            quickFix = object : TroubleshootQuickFix(CommonStrings.settings_troubleshoot_test_token_registration_quick_fix) {
                override fun doFix() {
                    context.lifecycleScope.launch(Dispatchers.IO) {
                        val workId = pushersManager.enqueueRegisterPusherWithFcmKey(fcmToken)
                        WorkManager.getInstance(context).getWorkInfoByIdLiveData(workId).observe(context, Observer { workInfo ->
                            if (workInfo != null) {
                                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                                    manager?.retry(testParameters)
                                } else if (workInfo.state == WorkInfo.State.FAILED) {
                                    manager?.retry(testParameters)
                                }
                            }
                        })
                    }
                }
            }

            status = TestStatus.FAILED
        } else {
            description = stringProvider.getString(CommonStrings.settings_troubleshoot_test_token_registration_success)
            status = TestStatus.SUCCESS
        }
    }
}
