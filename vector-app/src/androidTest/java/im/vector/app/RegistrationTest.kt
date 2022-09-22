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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import im.vector.app.features.MainActivity
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@LargeTest
class RegistrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun simpleRegister() {
        val userId: String = "test-${Random.nextLong()}"
        val password: String = "password"
        val homeServerUrl: String = "http://10.0.2.2:8080"

        // Check splashscreen is there
        onView(withId(R.id.loginSplashSubmit))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.login_splash_create_account)))

        // Click on get started
        onView(withId(R.id.loginSplashSubmit))
                .perform(click())

        // Check that use case screen is shown
        onView(withId(R.id.useCaseHeaderTitle))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.ftue_auth_use_case_title)))

        // Chose friends and family
        onView(withId(R.id.useCaseOptionOne))
                .perform(click())

        // Wait for matrix.org data to be retrieved
        onView(ViewMatchers.isRoot())
                .perform(waitForView(withId(R.id.editServerButton)))
        // Edit server url
        onView(withId(R.id.editServerButton))
                .perform(click())

        // Enter local synapse
        onView(withId(R.id.chooseServerInputEditText))
                .perform()
                // Text is not empty so use `replaceText` instead of `typeText`
                .perform(replaceText(homeServerUrl))

        // Click on continue
        onView(withId(R.id.chooseServerSubmit))
                .check(matches(isEnabled()))
                .perform(closeSoftKeyboard(), click())

        // Ensure password flow supported (wait for data to be retrieved)
        onView(ViewMatchers.isRoot())
                .perform(waitForView(withId(R.id.createAccountHeaderTitle)))

        onView(withId(R.id.createAccountEditText))
                .check(matches(isDisplayed()))
        onView(withId(R.id.createAccountPassword))
                .check(matches(isDisplayed()))

        // Type user id
        onView(withId(R.id.createAccountEditText))
                .perform(typeText(userId))

        // Ensure login button not yet enabled
        onView(withId(R.id.createAccountSubmit))
                .check(matches(not(isEnabled())))

        // Type password
        onView(withId(R.id.createAccountPassword))
                .perform(closeSoftKeyboard(), typeText(password))

        // Submit
        onView(withId(R.id.createAccountSubmit))
                .check(matches(isEnabled()))
                .perform(closeSoftKeyboard(), click())

        // Personalization
        onView(ViewMatchers.isRoot())
                .perform(waitForView(withId(R.id.accountCreatedTakeMeHome)))
        onView(withId(R.id.accountCreatedTakeMeHome))
                .perform(click())

        // Analytics
        onView(ViewMatchers.isRoot())
                .perform(waitForView(withId(R.id.submit)))
        onView(withId(R.id.submit))
                .perform(click())
    }
}
