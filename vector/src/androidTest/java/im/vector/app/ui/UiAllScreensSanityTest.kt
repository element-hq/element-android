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
import com.schibsted.spain.barista.assertion.BaristaListAssertions.assertListItemCount
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickBack
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.longClickOn
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
import im.vector.app.espresso.tools.clickOnPreference
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.features.MainActivity
import im.vector.app.features.createdirect.CreateDirectRoomActivity
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.home.room.detail.RoomDetailActivity
import im.vector.app.features.login.LoginActivity
import im.vector.app.features.roomdirectory.RoomDirectoryActivity
import im.vector.app.initialSyncIdlingResource
import im.vector.app.waitForView
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
        longClickOnMessageTest()

        // Menu
        openMenu()
        pressBack()
        clickMenu(R.id.voice_call)
        pressBack()
        clickMenu(R.id.video_call)
        pressBack()
        clickMenu(R.id.search)
        pressBack()

        pressBack()
    }

    private fun longClickOnMessageTest() {
        // Test quick reaction
        longClickOnMessage()
        // Add quick reaction
        clickOn("\uD83D\uDC4D️") // 👍

        sleep(1000)

        // Open reactions
        longClickOn("\uD83D\uDC4D️") // 👍
        pressBack()

        // Test add reaction
        longClickOnMessage()
        clickOn(R.string.message_add_reaction)
        // Filter
        // TODO clickMenu(R.id.search)
        clickListItem(R.id.emojiRecyclerView, 4)

        // Test Edit mode
        longClickOnMessage()
        clickOn(R.string.edit)
        // TODO Cancel action
        writeTo(R.id.composerEditText, "Hello universe!")
        // Wait a bit for the keyboard layout to update
        sleep(30)
        clickOn(R.id.sendButton)
        // Open edit history
        longClickOnMessage("Hello universe! (edited)")
        clickOn(R.string.message_view_edit_history)
        pressBack()
    }

    private fun longClickOnMessage(text: String = "Hello world!") {
        onView(withId(R.id.timelineRecyclerView))
                .perform(
                        actionOnItem<RecyclerView.ViewHolder>(
                                hasDescendant(withText(text)),
                                longClick()
                        )
                )
    }

    private fun navigateToRoomSettings() {
        clickOn(R.id.roomToolbarTitleView)
        assertDisplayed(R.id.roomProfileAvatarView)

        // Room settings
        clickListItem(R.id.matrixProfileRecyclerView, 3)
        navigateToRoomParameters()
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

        // Advanced
        // Room addresses
        clickListItem(R.id.matrixProfileRecyclerView, 15)
        onView(isRoot()).perform(waitForView(withText(R.string.room_alias_published_alias_title)))
        pressBack()

        // Room permissions
        clickListItem(R.id.matrixProfileRecyclerView, 17)
        onView(isRoot()).perform(waitForView(withText(R.string.room_permissions_title)))
        clickOn(R.string.room_permissions_change_room_avatar)
        clickDialogNegativeButton()
        // Toggle
        clickOn(R.string.show_advanced)
        clickOn(R.string.hide_advanced)
        pressBack()

        // Menu share
        // clickMenu(R.id.roomProfileShareAction)
        // pressBack()

        pressBack()
    }

    private fun navigateToRoomParameters() {
        // Room history readability
        clickListItem(R.id.roomSettingsRecyclerView, 4)
        pressBack()

        // Room access
        clickListItem(R.id.roomSettingsRecyclerView, 6)
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
        clickListItem(R.id.roomSettingsRecyclerView, 1)
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
            onView(withId(R.id.userListRecyclerView))
                    .perform(waitForView(withText(R.string.qr_code)))
            onView(withId(R.id.userListRecyclerView))
                    .perform(waitForView(withText(R.string.invite_friends)))
        }

        closeSoftKeyboard()
        pressBack()
        pressBack()
    }

    private fun navigateToSettings() {
        clickOn(R.id.groupToolbarAvatarImageView)
        clickOn(R.id.homeDrawerHeaderSettingsView)

        clickOn(R.string.settings_general_title)
        navigateToSettingsGeneral()
        pressBack()

        clickOn(R.string.settings_notifications)
        navigateToSettingsNotifications()
        pressBack()

        clickOn(R.string.settings_preferences)
        navigateToSettingsPreferences()
        pressBack()

        clickOn(R.string.preference_voice_and_video)
        pressBack()

        clickOn(R.string.settings_ignored_users)
        pressBack()

        clickOn(R.string.settings_security_and_privacy)
        navigateToSettingsSecurity()
        pressBack()

        clickOn(R.string.room_settings_labs_pref_title)
        pressBack()

        clickOn(R.string.settings_advanced_settings)
        navigateToSettingsAdvanced()
        pressBack()

        clickOn(R.string.preference_root_help_about)
        navigateToSettingsHelp()
        pressBack()

        pressBack()
    }

    private fun navigateToSettingsHelp() {
        /*
        clickOn(R.string.settings_app_info_link_title)
        Cannot go back...
        pressBack()
        clickOn(R.string.settings_copyright)
        pressBack()
        clickOn(R.string.settings_app_term_conditions)
        pressBack()
        clickOn(R.string.settings_privacy_policy)
        pressBack()
         */
        clickOn(R.string.settings_third_party_notices)
        clickDialogPositiveButton()
    }

    private fun navigateToSettingsAdvanced() {
        clickOnPreference(R.string.settings_notifications_targets)
        pressBack()

        clickOnPreference(R.string.settings_push_rules)
        pressBack()

        /* TODO P2 test developer screens
        // Enable developer mode
        clickOnSwitchPreference("SETTINGS_DEVELOPER_MODE_PREFERENCE_KEY")

        clickOnPreference(R.string.settings_account_data)
        clickOn("m.push_rules")
        pressBack()
        pressBack()
        clickOnPreference(R.string.settings_key_requests)
        pressBack()

        // Disable developer mode
        clickOnSwitchPreference("SETTINGS_DEVELOPER_MODE_PREFERENCE_KEY")
         */
    }

    private fun navigateToSettingsSecurity() {
        clickOnPreference(R.string.settings_active_sessions_show_all)
        pressBack()

        clickOnPreference(R.string.encryption_message_recovery)
        // TODO go deeper here
        pressBack()
        /* Cannot exit
        clickOnPreference(R.string.encryption_export_e2e_room_keys)
        pressBack()
         */
    }

    private fun navigateToSettingsPreferences() {
        clickOn(R.string.settings_interface_language)
        onView(isRoot())
                .perform(waitForView(withText("Dansk (Danmark)")))
        pressBack()
        clickOn(R.string.settings_theme)
        clickDialogNegativeButton()
        clickOn(R.string.font_size)
        clickDialogNegativeButton()
    }

    private fun navigateToSettingsNotifications() {
        clickOn(R.string.settings_notification_advanced)
        pressBack()
        /*
        clickOn(R.string.settings_noisy_notifications_preferences)
        TODO Cannot go back
        pressBack()
        clickOn(R.string.settings_silent_notifications_preferences)
        pressBack()
        clickOn(R.string.settings_call_notifications_preferences)
        pressBack()
         */
        clickOnPreference(R.string.settings_notification_troubleshoot)
        pressBack()
    }

    private fun navigateToSettingsGeneral() {
        clickOn(R.string.settings_profile_picture)
        clickDialogPositiveButton()
        clickOn(R.string.settings_display_name)
        clickDialogNegativeButton()
        clickOn(R.string.settings_password)
        clickDialogNegativeButton()
        clickOn(R.string.settings_emails_and_phone_numbers_title)
        pressBack()
        clickOn(R.string.settings_discovery_manage)
        clickOn(R.string.add_identity_server)
        pressBack()
        pressBack()
        // Homeserver
        clickOnPreference(R.string.settings_home_server)
        pressBack()
        // Identity server
        clickOnPreference(R.string.settings_identity_server)
        pressBack()
        // Deactivate account
        clickOnPreference(R.string.settings_deactivate_my_account)
        pressBack()
    }
}
