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

package im.vector.app

import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.internal.viewaction.SleepViewAction
import im.vector.app.features.MainActivity
import im.vector.app.ui.robot.ElementRobot
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@LargeTest
class CantVerifyTest {

    @get:Rule
    val testRule = RuleChain
            .outerRule(ActivityScenarioRule(MainActivity::class.java))
            .around(ClearCurrentSessionRule())

    private val elementRobot = ElementRobot()
    var userName: String = "loginTest_${UUID.randomUUID()}"

    @Test
    fun checkCantVerifyPopup() {
        // Let' create an account
        // This first session will create cross signing keys then logout
        elementRobot.signUp(userName)
        Espresso.onView(ViewMatchers.isRoot()).perform(SleepViewAction.sleep(2000))

        elementRobot.signout(false)
        Espresso.onView(ViewMatchers.isRoot()).perform(SleepViewAction.sleep(2000))

        // Let's login again now
        // There are no methods to verify (no other devices, nor 4S)
        // So it should ask to reset all
        elementRobot.login(userName)

        val activity = EspressoHelper.getCurrentActivity()!!
        Espresso.onView(ViewMatchers.isRoot())
                .perform(waitForView(ViewMatchers.withText(R.string.crosssigning_cannot_verify_this_session)))

        // check that the text is correct
        val popup = activity.findViewById<View>(com.tapadoo.alerter.R.id.llAlertBackground)!!
        activity.runOnUiThread { popup.performClick() }

        // ensure that it's the 4S setup bottomsheet
        Espresso.onView(ViewMatchers.withId(R.id.bottomSheetFragmentContainer))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.isRoot()).perform(SleepViewAction.sleep(2000))

        Espresso.onView(ViewMatchers.withText(R.string.bottom_sheet_setup_secure_backup_title))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}
