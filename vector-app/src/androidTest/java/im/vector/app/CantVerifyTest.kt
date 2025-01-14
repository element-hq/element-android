/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.lib.strings.CommonStrings
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
                .perform(waitForView(ViewMatchers.withText(CommonStrings.crosssigning_cannot_verify_this_session)))

        // check that the text is correct
        val popup = activity.findViewById<View>(com.tapadoo.alerter.R.id.llAlertBackground)!!
        activity.runOnUiThread { popup.performClick() }

        // ensure that it's the 4S setup bottomsheet
        Espresso.onView(ViewMatchers.withId(R.id.bottomSheetFragmentContainer))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.isRoot()).perform(SleepViewAction.sleep(2000))

        Espresso.onView(ViewMatchers.withText(CommonStrings.bottom_sheet_setup_secure_backup_title))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}
