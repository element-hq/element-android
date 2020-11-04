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
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.schibsted.spain.barista.assertion.BaristaListAssertions.assertListItemCount
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickBack
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import com.schibsted.spain.barista.interaction.BaristaDialogInteractions.clickDialogNegativeButton
import com.schibsted.spain.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.schibsted.spain.barista.interaction.BaristaEditTextInteractions.writeTo
import com.schibsted.spain.barista.interaction.BaristaListInteractions.clickListItem
import com.schibsted.spain.barista.interaction.BaristaListInteractions.clickListItemChild
import com.schibsted.spain.barista.interaction.BaristaMenuClickInteractions.clickMenu
import com.schibsted.spain.barista.interaction.BaristaMenuClickInteractions.openMenu
import im.vector.app.EspressoHelper
import im.vector.app.R
import im.vector.app.SleepViewAction
import im.vector.app.activityIdlingResource
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.features.MainActivity
import im.vector.app.features.createdirect.CreateDirectRoomActivity
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.home.room.detail.RoomDetailActivity
import im.vector.app.features.login.LoginActivity
import im.vector.app.features.roomdirectory.RoomDirectoryActivity
import im.vector.app.initialSyncIdlingResource
import im.vector.app.withIdlingResource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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
        navigateToSettings()

        // Create DM
        clickOn(R.id.bottom_action_people)
        createDm()

        // Create Room
        // First navigate to the other tab
        clickOn(R.id.bottom_action_rooms)
        createRoom()

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
    }

    private fun ignoreVerification() {
        Thread.sleep(6000)
        val activity = EspressoHelper.getCurrentActivity()!!

        val popup = activity.findViewById<View>(com.tapadoo.alerter.R.id.llAlertBackground)
        activity.runOnUiThread {
            popup.performClick()
        }

        assertDisplayed(R.id.bottomSheetFragmentContainer)

        onView(ViewMatchers.isRoot()).perform(SleepViewAction.sleep(2000))

        clickOn(R.string.skip)
        assertDisplayed(R.string.are_you_sure)
        clickOn(R.string.skip)
    }

    private fun createRoom() {
        clickOn(R.id.createGroupRoomButton)
        waitUntilActivityVisible<RoomDirectoryActivity> {
            assertDisplayed(R.id.publicRoomsList)
        }
        clickOn(R.string.create_new_room)

        // Create
        assertListItemCount(R.id.createRoomForm, 10)
        clickListItemChild(R.id.createRoomForm, 9, R.id.form_submit_button)

        waitUntilActivityVisible<RoomDetailActivity> {
            assertDisplayed(R.id.roomDetailContainer)
        }

        clickOn(R.id.attachmentButton)
        clickBack()

        // Send a message
        writeTo(R.id.composerEditText, "Hello world!")
        clickOn(R.id.sendButton)

        navigateToRoomSettings()

        // Long click on the message
        onView(withId(R.id.timelineRecyclerView))
                .perform(
                        actionOnItem<RecyclerView.ViewHolder>(
                                hasDescendant(withText("Hello world!")),
                                longClick()
                        )
                )
        pressBack()

        // Menu
        openMenu()
        pressBack()
        clickMenu(R.id.voice_call)
        pressBack()
        clickMenu(R.id.video_call)
        pressBack()

        pressBack()
    }

    private fun navigateToRoomSettings() {
        clickOn(R.id.roomToolbarTitleView)
        assertDisplayed(R.id.roomProfileAvatarView)

        // Room settings
        clickListItem(R.id.matrixProfileRecyclerView, 3)
        pressBack()

        // Notifications
        clickListItem(R.id.matrixProfileRecyclerView, 5)
        pressBack()

        assertDisplayed(R.id.roomProfileAvatarView)

        // People
        clickListItem(R.id.matrixProfileRecyclerView, 7)
        assertDisplayed(R.id.inviteUsersButton)
        navigateToRoomPeople()
        // Fab
        navigateToInvite()
        pressBack()
        pressBack()

        assertDisplayed(R.id.roomProfileAvatarView)

        // Uploads
        clickListItem(R.id.matrixProfileRecyclerView, 9)
        // File tab
        clickOn(R.string.uploads_files_title)
        pressBack()

        assertDisplayed(R.id.roomProfileAvatarView)

        // Leave
        clickListItem(R.id.matrixProfileRecyclerView, 13)
        clickDialogNegativeButton()

        // Menu share
        // clickMenu(R.id.roomProfileShareAction)
        // pressBack()

        pressBack()
    }

    private fun navigateToInvite() {
        assertDisplayed(R.id.inviteUsersButton)
        clickOn(R.id.inviteUsersButton)
        closeSoftKeyboard()
        pressBack()
    }

    private fun navigateToRoomPeople() {
        // Open first user
        clickListItem(R.id.recyclerView, 1)
        assertDisplayed(R.id.memberProfilePowerLevelView)

        // Verification
        clickListItem(R.id.matrixProfileRecyclerView, 1)
        clickBack()

        // Role
        clickListItem(R.id.matrixProfileRecyclerView, 3)
        clickDialogNegativeButton()

        clickBack()
    }

    private fun createDm() {
        clickOn(R.id.createChatRoomButton)

        withIdlingResource(activityIdlingResource(CreateDirectRoomActivity::class.java)) {
            assertDisplayed(R.id.addByMatrixId)
        }

        closeSoftKeyboard()
        pressBack()
        pressBack()
    }

    private fun navigateToSettings() {
        clickOn(R.id.groupToolbarAvatarImageView)
        clickOn(R.id.homeDrawerHeaderSettingsView)

        clickOn(R.string.settings_general_title)
        // TODO
        pressBack()

        clickOn(R.string.settings_notifications)
        // TODO
        pressBack()

        clickOn(R.string.settings_preferences)
        // TODO
        pressBack()

        clickOn(R.string.preference_voice_and_video)
        // TODO
        pressBack()

        clickOn(R.string.settings_ignored_users)
        // TODO
        pressBack()

        clickOn(R.string.settings_security_and_privacy)
        // TODO
        pressBack()

        clickOn(R.string.room_settings_labs_pref_title)
        // TODO
        pressBack()

        clickOn(R.string.settings_advanced_settings)
        // TODO
        pressBack()

        clickOn(R.string.preference_root_help_about)
        // TODO
        pressBack()

        pressBack()
    }
}
