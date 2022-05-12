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

package im.vector.app.ui.robot

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertDisabled
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertEnabled
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.DefaultVectorFeatures
import im.vector.app.waitForView

class OnboardingRobot {

    fun crawl() {
        waitUntilViewVisible(withId(R.id.loginSplashSubmit))
        crawlGetStarted()
        crawlAlreadyHaveAccount()
    }

    private fun crawlGetStarted() {
        clickOn(R.id.loginSplashSubmit)
        assertDisplayed(R.id.useCaseHeaderTitle, R.string.ftue_auth_use_case_title)
        clickOn(R.id.useCaseOptionOne)
        OnboardingServersRobot().crawlSignUp()
        pressBack()
        pressBack()
    }

    private fun crawlAlreadyHaveAccount() {
        clickOn(R.id.loginSplashAlreadyHaveAccount)
        OnboardingServersRobot().crawlSignIn()
        pressBack()
    }

    fun createAccount(userId: String, password: String = "password", homeServerUrl: String = "http://10.0.2.2:8080") {
        initSession(true, userId, password, homeServerUrl)
        waitUntilViewVisible(withText(R.string.ftue_account_created_congratulations_title))
        if (DefaultVectorFeatures().isOnboardingPersonalizeEnabled()) {
            clickOn(R.string.ftue_account_created_personalize)

            waitUntilViewVisible(withText(R.string.ftue_display_name_title))
            writeTo(R.id.displayNameInput, "UI automation")
            clickOn(R.string.ftue_personalize_submit)

            waitUntilViewVisible(withText(R.string.ftue_profile_picture_title))
            clickOn(R.string.ftue_personalize_skip_this_step)

            waitUntilViewVisible(withText(R.string.ftue_personalize_complete_title))
            clickOn(R.string.ftue_personalize_lets_go)
        } else {
            clickOn(R.string.ftue_account_created_take_me_home)
        }
    }

    fun login(userId: String, password: String = "password", homeServerUrl: String = "http://10.0.2.2:8080") {
        initSession(false, userId, password, homeServerUrl)
    }

    private fun initSession(createAccount: Boolean,
                            userId: String,
                            password: String,
                            homeServerUrl: String) {
        waitUntilViewVisible(withId(R.id.loginSplashSubmit))
        assertDisplayed(R.id.loginSplashSubmit, R.string.login_splash_create_account)
        if (createAccount) {
            clickOn(R.id.loginSplashSubmit)
            clickOn(R.id.useCaseOptionOne)
        } else {
            clickOn(R.id.loginSplashAlreadyHaveAccount)
        }
        assertDisplayed(R.id.loginServerTitle, R.string.login_server_title)
        // Chose custom server
        clickOn(R.id.loginServerChoiceOther)
        // Enter local synapse
        writeTo(R.id.loginServerUrlFormHomeServerUrl, homeServerUrl)
        assertEnabled(R.id.loginServerUrlFormSubmit)
        closeSoftKeyboard()
        clickOn(R.id.loginServerUrlFormSubmit)
        onView(isRoot()).perform(waitForView(withId(R.id.loginField)))

        // Ensure password flow supported
        assertDisplayed(R.id.loginField)
        assertDisplayed(R.id.passwordField)

        writeTo(R.id.loginField, userId)
        assertDisabled(R.id.loginSubmit)
        writeTo(R.id.passwordField, password)
        assertEnabled(R.id.loginSubmit)

        closeSoftKeyboard()
        clickOn(R.id.loginSubmit)
    }
}
