/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.ui

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogNegativeButton
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import im.vector.app.EspressoHelper
import im.vector.app.R
import im.vector.app.SleepViewAction
import im.vector.app.activityIdlingResource
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.features.MainActivity
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.login.LoginActivity
import im.vector.app.initialSyncIdlingResource
import im.vector.app.ui.robot.ElementRobot
import im.vector.app.withIdlingResource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep
import java.util.UUID

/**
 * This test aim to open every possible screen of the application
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class UiAllScreensSanityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val uiTestBase = UiTestBase()
    private val elementRobot = ElementRobot()

    // Last passing:
    // 2020-11-09
    // 2020-12-16 After ViewBinding huge change
    // 2021-04-08 Testing 429 change
    @Test
    fun allScreensTest() {
        // Create an account
        val userId = "UiTest_" + UUID.randomUUID().toString()
        uiTestBase.createAccount(userId = userId)

        withIdlingResource(activityIdlingResource(HomeActivity::class.java)) {
            assertDisplayed(R.id.roomListContainer)
            closeSoftKeyboard()
        }

        val activity = EspressoHelper.getCurrentActivity()!!
        val uiSession = (activity as HomeActivity).activeSessionHolder.getActiveSession()

        withIdlingResource(initialSyncIdlingResource(uiSession)) {
            assertDisplayed(R.id.roomListContainer)
        }

        assertDisplayed(R.id.bottomNavigationView)

        // Settings
        elementRobot.settings {
            general { crawl() }
            notifications { crawl() }
            preferences { crawl() }
            voiceAndVideo()
            ignoredUsers()
            securityAndPrivacy { crawl() }
            labs()
            advancedSettings { crawl() }
            helpAndAbout { crawl() }
        }

        elementRobot.newDirectMessage {
            verifyQrCodeButton()
            verifyInviteFriendsButton()
        }

        assertDisplayed(R.id.bottomNavigationView)
        sleep(1000)

        elementRobot.newRoom {
            createNewRoom {
                crawl()
                createRoom {
                    postMessage("Hello world!")
                    crawl()
                    openSettings { crawl() }
                }
            }
        }

        assertDisplayed(R.id.bottomNavigationView)

        // Long click on the room
        onView(withId(R.id.roomListView))
                .perform(
                        actionOnItem<RecyclerView.ViewHolder>(
                                hasDescendant(withText(R.string.room_displayname_empty_room)),
                                longClick()
                        )
                )
        pressBack()

        uiTestBase.signout()

        // We have sent a message in a e2e room, accept to loose it
        clickOn(R.id.exitAnywayButton)
        // Dark pattern
        clickDialogNegativeButton()

        // Login again on the same account
        waitUntilActivityVisible<LoginActivity> {
            assertDisplayed(R.id.loginSplashLogo)
        }

        uiTestBase.login(userId)
        ignoreVerification()

        uiTestBase.signout()
        clickDialogPositiveButton()

        // TODO Deactivate account instead of logout?
    }

    private fun ignoreVerification() {
        sleep(6000)
        val activity = EspressoHelper.getCurrentActivity()!!

        val popup = activity.findViewById<View>(com.tapadoo.alerter.R.id.llAlertBackground)
        activity.runOnUiThread {
            popup.performClick()
        }

        assertDisplayed(R.id.bottomSheetFragmentContainer)

        onView(isRoot()).perform(SleepViewAction.sleep(2000))

        clickOn(R.string.skip)
        assertDisplayed(R.string.are_you_sure)
        clickOn(R.string.skip)
    }
}

