/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.settings.troubleshoot

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.pushers.PushersManager
import im.vector.app.core.pushers.UnifiedPushHelper
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.session.pushers.PusherState
import javax.inject.Inject

class TestEndpointAsTokenRegistration @Inject constructor(
        private val context: FragmentActivity,
        private val stringProvider: StringProvider,
        private val pushersManager: PushersManager,
        private val activeSessionHolder: ActiveSessionHolder,
        private val unifiedPushHelper: UnifiedPushHelper,
) : TroubleshootTest(R.string.settings_troubleshoot_test_endpoint_registration_title) {

    override fun perform(testParameters: TestParameters) {
        // Check if we have a registered pusher for this token
        val endpoint = unifiedPushHelper.getEndpointOrToken() ?: run {
            status = TestStatus.FAILED
            return
        }
        val session = activeSessionHolder.getSafeActiveSession() ?: run {
            status = TestStatus.FAILED
            return
        }
        val pushers = session.pushersService().getPushers().filter {
            it.pushKey == endpoint && it.state == PusherState.REGISTERED
        }
        if (pushers.isEmpty()) {
            description = stringProvider.getString(
                    R.string.settings_troubleshoot_test_endpoint_registration_failed,
                    stringProvider.getString(R.string.sas_error_unknown)
            )
            quickFix = object : TroubleshootQuickFix(R.string.settings_troubleshoot_test_endpoint_registration_quick_fix) {
                override fun doFix() {
                    unifiedPushHelper.forceRegister(
                            context,
                            pushersManager
                    )
                    val workId = pushersManager.enqueueRegisterPusherWithFcmKey(endpoint)
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

            status = TestStatus.FAILED
        } else {
            description = stringProvider.getString(R.string.settings_troubleshoot_test_endpoint_registration_success)
            status = TestStatus.SUCCESS
        }
    }
}
