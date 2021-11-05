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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import im.vector.app.EspressoHelper
import im.vector.app.R
import im.vector.app.SleepViewAction
import im.vector.app.features.MainActivity
import im.vector.app.ui.robot.ElementRobot
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

    private val elementRobot = ElementRobot()

    // Last passing:
    // 2020-11-09
    // 2020-12-16 After ViewBinding huge change
    // 2021-04-08 Testing 429 change
    @Test
    fun allScreensTest() {
        // Create an account
        val userId = "UiTest_" + UUID.randomUUID().toString()
        elementRobot.login(userId)

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

        elementRobot.roomList {
            verifyCreatedRoom()
        }

// Disable until the "you don't have a session for id %d" sign out bug is fixed
//        elementRobot.signout()
////        Login again on the same account
//        elementRobot.login(userId)
//
//        ignoreVerification()
//
//        elementRobot.signout()
//        clickDialogPositiveButton()

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

