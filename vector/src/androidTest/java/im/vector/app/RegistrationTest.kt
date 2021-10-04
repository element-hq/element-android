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

package im.vector.app

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import im.vector.app.features.MainActivity
import im.vector.app.features.home.HomeActivity
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class RegistrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun simpleRegister() {
        val userId: String = "UiAutoTest_${System.currentTimeMillis()}"
        val password: String = "password"
        val homeServerUrl: String = "http://10.0.2.2:8080"

        // Check splashscreen is there
        onView(withId(R.id.loginSplashSubmit))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.login_splash_submit)))

        // Click on get started
        onView(withId(R.id.loginSplashSubmit))
                .perform(click())

        // Check that homeserver options are shown
        onView(withId(R.id.loginServerTitle))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.login_server_title)))

        // Chose custom server
        onView(withId(R.id.loginServerChoiceOther))
                .perform(click())

        // Enter local synapse
        onView(withId(R.id.loginServerUrlFormHomeServerUrl))
                .perform(typeText(homeServerUrl))

        // Click on continue
        onView(withId(R.id.loginServerUrlFormSubmit))
                .check(matches(isEnabled()))
                .perform(closeSoftKeyboard(), click())

        // Click on the signup button
        onView(withId(R.id.loginSignupSigninSubmit))
                .check(matches(isDisplayed()))
                .perform(click())

        // Ensure password flow supported
        onView(withId(R.id.loginField))
                .check(matches(isDisplayed()))
        onView(withId(R.id.passwordField))
                .check(matches(isDisplayed()))

        // Ensure user id
        onView(withId(R.id.loginField))
                .perform(typeText(userId))

        // Ensure login button not yet enabled
        onView(withId(R.id.loginSubmit))
                .check(matches(not(isEnabled())))

        // Ensure password
        onView(withId(R.id.passwordField))
                .perform(closeSoftKeyboard(), typeText(password))

        // Submit
        onView(withId(R.id.loginSubmit))
                .check(matches(isEnabled()))
                .perform(closeSoftKeyboard(), click())

        withIdlingResource(activityIdlingResource(HomeActivity::class.java)) {
            onView(withId(R.id.roomListContainer))
                    .check(matches(isDisplayed()))
        }

        val activity = EspressoHelper.getCurrentActivity()!!
        val uiSession = (activity as HomeActivity).activeSessionHolder.getActiveSession()

        // Wait for initial sync and check room list is there
        withIdlingResource(initialSyncIdlingResource(uiSession)) {
            onView(withId(R.id.roomListContainer))
                    .check(matches(isDisplayed()))
        }
    }
}
