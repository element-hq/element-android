/*
 * Copyright 2020 New Vector Ltd
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

import androidx.appcompat.app.AppCompatActivity
import im.vector.app.R
import im.vector.app.core.pushers.PushersManager
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.settings.troubleshoot.TroubleshootTest
import im.vector.app.push.fcm.FcmHelper
import org.matrix.android.sdk.api.MatrixCallback
import javax.inject.Inject

/**
 * Test Push by asking Sygnal to send a Push back
 */
class TestPushFromSygnal @Inject constructor(private val context: AppCompatActivity,
                                             private val stringProvider: StringProvider,
                                             private val pushersManager: PushersManager)
    : TroubleshootTest(R.string.settings_troubleshoot_test_push_loop_title) {

    override fun perform() {
        val fcmToken = FcmHelper.getFcmToken(context) ?: run {
            status = TestStatus.FAILED
            return
        }
        pushersManager.testPush(fcmToken, object : MatrixCallback<Unit> {
            override fun onFailure(failure: Throwable) {
                description = stringProvider.getString(R.string.settings_troubleshoot_test_push_loop_failed)
                status = TestStatus.FAILED
            }

            override fun onSuccess(data: Unit) {
                description = stringProvider.getString(R.string.settings_troubleshoot_test_push_loop_success)
                status = TestStatus.SUCCESS
            }
        })
    }
}
