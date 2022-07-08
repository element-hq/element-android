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

package im.vector.app.ui.robot

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.PerformException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText

import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.internal.performAction
import com.adevinta.android.barista.internal.util.resourceMatcher
import im.vector.app.R
import im.vector.app.espresso.tools.clickOnPreference
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.espresso.tools.waitUntilDialogVisible
import im.vector.app.features.analytics.ui.consent.AnalyticsOptInActivity
import im.vector.app.waitForView
import im.vector.app.withRetry
import org.hamcrest.core.AllOf
import org.hamcrest.core.AnyOf
import java.util.EnumSet.allOf

class CryptoRobot {


    // Do this if we don't get the popup in time; but actually; just wait for the pop-up
    fun manualVerification() {
        // Settings
        BaristaDrawerInteractions.openDrawer()
        clickOn(R.id.homeDrawerHeaderSettingsView)
        BaristaClickInteractions.clickOn(R.string.settings_security_and_privacy)
        clickOnPreference(R.string.settings_active_sessions_show_all)
        clickListItem(R.id.genericRecyclerView, 3); // the first remote client. // TODO rename yourself when you create
        AllOf.allOf(withId(R.id.itemVerificationClickableZone),withChild(withText(R.string.verification_verify_device))).performAction(click())
            }

    fun startVerification() {
        // These are kinda async popups, be somewhat lenient with them.
        withRetry {
            waitUntilViewVisible(withText(R.string.crosssigning_verify_this_session))
            clickOn(R.string.crosssigning_verify_this_session)
        }
    }

    fun acceptVerification() {
        // This is somewhat async; be lenient

        withRetry {
            waitUntilViewVisible(withText(R.string.verification_request))
            clickOn(R.string.verification_request)
        }
        try {
            withRetry {

                waitUntilViewVisible(withText(R.string.verification_scan_emoji_title))
                clickOn(R.string.verification_scan_emoji_title);
            }
        } catch(e: PerformException) {
            // Ignore...
        }
    }

    fun completeVerification() {
        waitForView(withText(R.string.verification_emoji_notice))
        clickOn(R.string.verification_sas_match)
        waitForView(withText(R.string.verification_conclusion_ok_self_notice))
        waitForView(withText(R.string.done))
        clickOn(R.string.done)
    }
}
