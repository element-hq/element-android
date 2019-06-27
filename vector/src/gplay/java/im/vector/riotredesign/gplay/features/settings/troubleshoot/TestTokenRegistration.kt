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
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.session.pushers.PusherState
import im.vector.riotredesign.R
import im.vector.riotredesign.core.pushers.PushersManager
import im.vector.riotredesign.features.settings.troubleshoot.TroubleshootTest
import im.vector.riotredesign.push.fcm.FcmHelper
import org.koin.android.ext.android.get

/**
 * Force registration of the token to HomeServer
 */
class TestTokenRegistration(val fragment: Fragment) : TroubleshootTest(R.string.settings_troubleshoot_test_token_registration_title) {

    override fun perform() {
        //Check if we have a registered pusher for this token
        val fcmToken = FcmHelper.getFcmToken(fragment.requireContext()) ?: run {
            status = TestStatus.FAILED
            return
        }

        val session = Matrix.getInstance().currentSession ?: run {
            status = TestStatus.FAILED
            return
        }

        val pusher = session.pushers().filter {
            it.pushKey == fcmToken && it.state == PusherState.REGISTERED
        }

        if (pusher == null) {
            description = fragment.getString(R.string.settings_troubleshoot_test_token_registration_failed, null)
            quickFix = object : TroubleshootQuickFix(R.string.settings_troubleshoot_test_token_registration_quick_fix) {
                override fun doFix() {
                    val workId = fragment.get<PushersManager>().registerPusherWithFcmKey(fcmToken)
                    WorkManager.getInstance().getWorkInfoByIdLiveData(workId).observe(fragment, Observer { workInfo ->
                        if (workInfo != null) {
                            if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                                manager?.retry()
                            } else if (workInfo.state == WorkInfo.State.FAILED) {
                                manager?.retry()
                            }
                        }
                    })
                }

            }

            status = TestStatus.FAILED

        } else {
            description = fragment.getString(R.string.settings_troubleshoot_test_token_registration_success)
            status = TestStatus.SUCCESS
        }

    }

}