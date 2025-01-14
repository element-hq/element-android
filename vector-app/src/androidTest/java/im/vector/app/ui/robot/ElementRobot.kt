/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot

import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogNegativeButton
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import im.vector.app.EspressoHelper
import im.vector.app.R
import im.vector.app.espresso.tools.clickOnPreference
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilDialogVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.createdirect.CreateDirectRoomActivity
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.onboarding.OnboardingActivity
import im.vector.app.features.settings.VectorSettingsActivity
import im.vector.app.initialSyncIdlingResource
import im.vector.app.ui.robot.settings.SettingsRobot
import im.vector.app.ui.robot.settings.labs.LabFeature
import im.vector.app.ui.robot.settings.labs.LabFeaturesPreferences
import im.vector.app.ui.robot.space.SpaceRobot
import im.vector.app.withIdlingResource
import im.vector.lib.strings.CommonStrings
import timber.log.Timber

class ElementRobot(
        private val labsPreferences: LabFeaturesPreferences = LabFeaturesPreferences(true)
) {
    fun onboarding(block: OnboardingRobot.() -> Unit) {
        block(OnboardingRobot())
    }

    fun signUp(userId: String) {
        val onboardingRobot = OnboardingRobot()
        onboardingRobot.createAccount(userId = userId)
        val analyticsRobot = AnalyticsRobot()
        analyticsRobot.optOut()
        waitForHome()
    }

    fun login(userId: String) {
        val onboardingRobot = OnboardingRobot()
        onboardingRobot.login(userId = userId)
        waitForHome()
    }

    private fun waitForHome() {
        waitUntilActivityVisible<HomeActivity> {
            waitUntilViewVisible(withId(R.id.roomListContainer))
        }
        val activity = EspressoHelper.getCurrentActivity()!!
        val uiSession = (activity as HomeActivity).activeSessionHolder.getActiveSession()
        withIdlingResource(initialSyncIdlingResource(uiSession)) {
            waitUntilViewVisible(withId(R.id.roomListContainer))
        }
    }

    fun settings(shouldGoBack: Boolean = true, block: SettingsRobot.() -> Unit) {
        if (labsPreferences.isNewAppLayoutEnabled) {
            onView(withId((R.id.avatar))).perform(click())
        } else {
            openDrawer()
            clickOn(R.id.homeDrawerHeaderSettingsView)
        }

        block(SettingsRobot())
        if (shouldGoBack) pressBack()
        waitUntilViewVisible(withId(R.id.roomListContainer))
    }

    fun layoutPreferences(block: LayoutPreferencesRobot.() -> Unit) {
        openActionBarOverflowOrOptionsMenu(
                ApplicationProvider.getApplicationContext()
        )
        clickOn(CommonStrings.home_layout_preferences)
        waitUntilDialogVisible(withId(R.id.home_layout_settings_recents))

        block(LayoutPreferencesRobot())

        pressBack()
    }

    fun newDirectMessage(block: NewDirectMessageRobot.() -> Unit) {
        if (labsPreferences.isNewAppLayoutEnabled) {
            clickOn(R.id.newLayoutCreateChatButton)
            waitUntilDialogVisible(withId(R.id.start_chat))
            clickOn(R.id.start_chat)
        } else {
            clickOn(R.id.bottom_action_people)
            clickOn(R.id.createChatRoomButton)
        }

        waitUntilActivityVisible<CreateDirectRoomActivity> {
            waitUntilViewVisible(withId(R.id.userListSearch))
        }
        closeSoftKeyboard()
        block(NewDirectMessageRobot())
        pressBack()
        waitUntilViewVisible(withId(R.id.roomListContainer))
    }

    fun newRoom(block: NewRoomRobot.() -> Unit) {
        if (!labsPreferences.isNewAppLayoutEnabled) {
            clickOn(R.id.bottom_action_rooms)
        }
        RoomListRobot(labsPreferences).newRoom { block() }
        waitUntilViewVisible(withId(R.id.roomListContainer))
    }

    fun roomList(block: RoomListRobot.() -> Unit) {
        if (!labsPreferences.isNewAppLayoutEnabled) {
            clickOn(R.id.bottom_action_rooms)
        }

        block(RoomListRobot(labsPreferences))
        waitUntilViewVisible(withId(R.id.roomListContainer))
    }

    fun toggleLabFeature(labFeature: LabFeature) {
        when (labFeature) {
            LabFeature.THREAD_MESSAGES -> {
                settings(shouldGoBack = false) {
                    labs(shouldGoBack = false) {
                        onView(withText(CommonStrings.labs_enable_thread_messages))
                                .check(ViewAssertions.matches(isDisplayed()))
                                .perform(ViewActions.closeSoftKeyboard(), click())
                    }
                }
                // at this point we are in a race with the app restarting. The steps that happen are:
                // - (initially) app has started, app has initial synched
                // - (restart) app has strted, app has not initial synched
                // - (racey) app shows some UI but overlays with initial sync ui
                // - (initial sync finishes) app has started, has initial synched

                // We need to wait for the initial sync to complete; but we can't
                // use waitForHome() like login does.

                // waitForHome() -- does not work because we have already fufilled the initialSync
                // so we can racily have an IllegalStateException that we have transitioned from busy -> idle
                // but never having sent the signal.

                // So we need to not start waiting for an initial sync until we have restarted
                // then we do need to wait for the sync to complete.

                // Which is convoluted especially as it involves the app state refreshing
                // so; in order to make this be more stable
                // I hereby cheat and write:
                Thread.sleep(30_000)
            }
            else -> {
            }
        }
    }

    fun signout(expectSignOutWarning: Boolean) {
        if (labsPreferences.isNewAppLayoutEnabled) {
            onView(withId((R.id.avatar)))
                    .perform(click())
            waitUntilActivityVisible<VectorSettingsActivity> {
                clickOn(CommonStrings.settings_general_title)
            }
            clickOnPreference(CommonStrings.action_sign_out)
        } else {
            clickOn(R.id.groupToolbarAvatarImageView)
            clickOn(R.id.homeDrawerHeaderSignoutView)
        }

        val isShowingSignOutWarning = kotlin.runCatching {
            waitUntilViewVisible(withId(R.id.exitAnywayButton))
        }.isSuccess

        if (expectSignOutWarning != isShowingSignOutWarning) {
            val expected = expectSignOutWarning.toWarningType()
            val actual = isShowingSignOutWarning.toWarningType()
            Timber.w("Unexpected sign out flow, expected warning to be: $expected but was $actual")
        }

        if (isShowingSignOutWarning) {
            // We have sent a message in a e2e room, accept to loose it
            clickOn(R.id.exitAnywayButton)
            // Dark pattern
            waitUntilDialogVisible(withId(android.R.id.button2))
            clickDialogNegativeButton()
        } else {
            waitUntilDialogVisible(withId(android.R.id.button1))
            clickDialogPositiveButton()
        }

        waitUntilActivityVisible<OnboardingActivity> {
            assertDisplayed(R.id.loginSplashSubmit)
        }
    }

    fun dismissVerificationIfPresent() {
        kotlin.runCatching {
            Thread.sleep(6000)
            val activity = EspressoHelper.getCurrentActivity()!!
            val popup = activity.findViewById<View>(com.tapadoo.alerter.R.id.llAlertBackground)!!
            activity.runOnUiThread { popup.performClick() }
            waitUntilViewVisible(withId(R.id.bottomSheetFragmentContainer))
            pressBack()
        }.onFailure { Timber.w(it, "Verification popup missing") }
    }

    fun space(block: SpaceRobot.() -> Unit) {
        block(SpaceRobot(labsPreferences))
    }
}

private fun Boolean.toWarningType() = if (this) "shown" else "skipped"

fun ElementRobot.withDeveloperMode(block: ElementRobot.() -> Unit) {
    settings { toggleDeveloperMode() }
    block()
    settings { toggleDeveloperMode() }
}
