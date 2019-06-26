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
package im.vector.riotredesign.push.fcm

import androidx.fragment.app.Fragment
import im.vector.matrix.android.api.session.Session
import im.vector.riotredesign.features.settings.troubleshoot.*
import im.vector.riotredesign.push.fcm.troubleshoot.TestFirebaseToken
import im.vector.riotredesign.push.fcm.troubleshoot.TestPlayServices
import im.vector.riotredesign.push.fcm.troubleshoot.TestTokenRegistration

class NotificationTroubleshootTestManagerFactory {

    companion object {
        fun createTestManager(fragment: Fragment, session: Session?): NotificationTroubleshootTestManager {
            val mgr = NotificationTroubleshootTestManager(fragment)
            mgr.addTest(TestSystemSettings(fragment))
            if (session != null) {
                mgr.addTest(TestAccountSettings(fragment, session))
            }
            mgr.addTest(TestDeviceSettings(fragment))
            if (session != null) {
                mgr.addTest(TestBingRulesSettings(fragment, session))
            }
            mgr.addTest(TestPlayServices(fragment))
            mgr.addTest(TestFirebaseToken(fragment))
            mgr.addTest(TestTokenRegistration(fragment))
            return mgr
        }
    }

}