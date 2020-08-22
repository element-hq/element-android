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
package im.vector.app.push.fcm

import androidx.fragment.app.Fragment
import im.vector.app.features.settings.troubleshoot.NotificationTroubleshootTestManager
import im.vector.app.features.settings.troubleshoot.TestAccountSettings
import im.vector.app.features.settings.troubleshoot.TestDeviceSettings
import im.vector.app.features.settings.troubleshoot.TestPushRulesSettings
import im.vector.app.features.settings.troubleshoot.TestSystemSettings
import im.vector.app.gplay.features.settings.troubleshoot.TestFirebaseToken
import im.vector.app.gplay.features.settings.troubleshoot.TestPlayServices
import im.vector.app.gplay.features.settings.troubleshoot.TestTokenRegistration
import javax.inject.Inject

class NotificationTroubleshootTestManagerFactory @Inject constructor(private val testSystemSettings: TestSystemSettings,
                                                                     private val testAccountSettings: TestAccountSettings,
                                                                     private val testDeviceSettings: TestDeviceSettings,
                                                                     private val testBingRulesSettings: TestPushRulesSettings,
                                                                     private val testPlayServices: TestPlayServices,
                                                                     private val testFirebaseToken: TestFirebaseToken,
                                                                     private val testTokenRegistration: TestTokenRegistration) {

    fun create(fragment: Fragment): NotificationTroubleshootTestManager {
        val mgr = NotificationTroubleshootTestManager(fragment)
        mgr.addTest(testSystemSettings)
        mgr.addTest(testAccountSettings)
        mgr.addTest(testDeviceSettings)
        mgr.addTest(testBingRulesSettings)
        mgr.addTest(testPlayServices)
        mgr.addTest(testFirebaseToken)
        mgr.addTest(testTokenRegistration)
        return mgr
    }
}
