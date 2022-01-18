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
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertDisabled
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertEnabled
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.waitForView

class OnboardingRobot {

    fun crawl() {
        waitUntilViewVisible(withId(R.id.loginSplashSubmit))
        crawlGetStarted()
        crawlAlreadyHaveAccount()
    }

    private fun crawlGetStarted() {
        clickOn(R.id.loginSplashSubmit)
        AuthOptionsRobot().crawlGetStarted()
        pressBack()
    }

    private fun crawlAlreadyHaveAccount() {
        clickOn(R.id.loginSplashAlreadyHaveAccount)
        AuthOptionsRobot().crawlAlreadyHaveAccount()
        pressBack()
    }

    fun createAccount(userId: String, password: String = "password", homeServerUrl: String = "http://10.0.2.2:8080") {
        initSession(true, userId, password, homeServerUrl)
    }

    fun login(userId: String, password: String = "password", homeServerUrl: String = "http://10.0.2.2:8080") {
        initSession(false, userId, password, homeServerUrl)
    }

    private fun initSession(createAccount: Boolean,
                            userId: String,
                            password: String,
                            homeServerUrl: String) {
        waitUntilViewVisible(withId(R.id.loginSplashSubmit))
        assertDisplayed(R.id.loginSplashSubmit, R.string.login_splash_submit)
        if (createAccount) {
            clickOn(R.id.loginSplashSubmit)
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

class AuthOptionsRobot {

    fun crawlGetStarted() {
        assertDisplayed(R.id.loginServerTitle, R.string.login_server_title)
        crawlMatrixServer(isSignUp = true)
        crawlEmsServer()
        crawlOtherServer(isSignUp = true)
        crawlSignInWithMatrixId()
    }

    fun crawlAlreadyHaveAccount() {
        assertDisplayed(R.id.loginServerTitle, R.string.login_server_title)
        crawlMatrixServer(isSignUp = false)
        crawlEmsServer()
        crawlOtherServer(isSignUp = false)
        crawlSignInWithMatrixId()
    }

    private fun crawlOtherServer(isSignUp: Boolean) {
        clickOn(R.id.loginServerChoiceOther)
        waitUntilViewVisible(withId(R.id.loginServerUrlFormTitle))
        writeTo(R.id.loginServerUrlFormHomeServerUrl, "https://chat.mozilla.org")
        clickOn(R.id.loginServerUrlFormSubmit)
        waitUntilViewVisible(withId(R.id.loginSignupSigninTitle))
        assertDisplayed(R.id.loginSignupSigninText, "Connect to chat.mozilla.org")
        assertDisplayed(R.id.loginSignupSigninSubmit, R.string.login_signin_sso)
        pressBack()

        writeTo(R.id.loginServerUrlFormHomeServerUrl, "https://matrix.org")
        clickOn(R.id.loginServerUrlFormSubmit)
        assetMatrixSignInOptions(isSignUp)
        pressBack()
        pressBack()
    }

    private fun crawlEmsServer() {
        clickOn(R.id.loginServerChoiceEms)
        waitUntilViewVisible(withId(R.id.loginServerUrlFormTitle))
        assertDisplayed(R.id.loginServerUrlFormTitle, R.string.login_connect_to_modular)

        writeTo(R.id.loginServerUrlFormHomeServerUrl, "https://one.ems.host")
        clickOn(R.id.loginServerUrlFormSubmit)

        waitUntilViewVisible(withId(R.id.loginSignupSigninTitle))
        assertDisplayed(R.id.loginSignupSigninText, "one.ems.host")
        assertDisplayed(R.id.loginSignupSigninSubmit, R.string.login_signin_sso)
        pressBack()
        pressBack()
    }

    private fun crawlMatrixServer(isSignUp: Boolean) {
        clickOn(R.id.loginServerChoiceMatrixOrg)
        assetMatrixSignInOptions(isSignUp)
        pressBack()
    }

    private fun assetMatrixSignInOptions(isSignUp: Boolean) {
        waitUntilViewVisible(withId(R.id.loginTitle))
        when (isSignUp) {
            true  -> assertDisplayed(R.id.loginTitle, "Sign up to matrix.org")
            false -> assertDisplayed(R.id.loginTitle, "Connect to matrix.org")
        }
    }

    private fun crawlSignInWithMatrixId() {
        clickOn(R.id.loginServerIKnowMyIdSubmit)
        waitUntilViewVisible(withId(R.id.loginTitle))
        assertDisplayed(R.id.loginTitle, R.string.login_signin_matrix_id_title)
        pressBack()
    }
}

