/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot

import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions
import com.adevinta.android.barista.interaction.BaristaClickInteractions
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.lib.strings.CommonStrings

class OnboardingServersRobot {

    fun crawlSignUp() {
        BaristaVisibilityAssertions.assertDisplayed(R.id.loginServerTitle, CommonStrings.login_server_title)
        crawlMatrixServer(isSignUp = true)
        crawlEmsServer()
        crawlOtherServer(isSignUp = true)
        crawlSignInWithMatrixId()
    }

    fun crawlSignIn() {
        BaristaVisibilityAssertions.assertDisplayed(R.id.loginServerTitle, CommonStrings.login_server_title)
        crawlMatrixServer(isSignUp = false)
        crawlEmsServer()
        crawlOtherServer(isSignUp = false)
        crawlSignInWithMatrixId()
    }

    private fun crawlOtherServer(isSignUp: Boolean) {
        BaristaClickInteractions.clickOn(R.id.loginServerChoiceOther)
        waitUntilViewVisible(ViewMatchers.withId(R.id.loginServerUrlFormTitle))
        BaristaEditTextInteractions.writeTo(R.id.loginServerUrlFormHomeServerUrl, "https://chat.mozilla.org")
        BaristaClickInteractions.clickOn(R.id.loginServerUrlFormSubmit)
        waitUntilViewVisible(ViewMatchers.withId(R.id.loginSignupSigninTitle))
        BaristaVisibilityAssertions.assertDisplayed(R.id.loginSignupSigninText, "Connect to chat.mozilla.org")
        BaristaVisibilityAssertions.assertDisplayed(R.id.loginSignupSigninSubmit, CommonStrings.login_signin_sso)
        Espresso.pressBack()

        BaristaEditTextInteractions.writeTo(R.id.loginServerUrlFormHomeServerUrl, "https://matrix.org")
        BaristaClickInteractions.clickOn(R.id.loginServerUrlFormSubmit)
        assetMatrixSignInOptions(isSignUp)
        Espresso.pressBack()
        Espresso.pressBack()
    }

    private fun crawlEmsServer() {
        BaristaClickInteractions.clickOn(R.id.loginServerChoiceEms)
        waitUntilViewVisible(ViewMatchers.withId(R.id.loginServerUrlFormTitle))
        BaristaVisibilityAssertions.assertDisplayed(R.id.loginServerUrlFormTitle, CommonStrings.login_connect_to_modular)

        BaristaEditTextInteractions.writeTo(R.id.loginServerUrlFormHomeServerUrl, "https://one.ems.host")
        BaristaClickInteractions.clickOn(R.id.loginServerUrlFormSubmit)

        waitUntilViewVisible(ViewMatchers.withId(R.id.loginSignupSigninTitle))
        BaristaVisibilityAssertions.assertDisplayed(R.id.loginSignupSigninText, "one.ems.host")
        BaristaVisibilityAssertions.assertDisplayed(R.id.loginSignupSigninSubmit, CommonStrings.login_signin_sso)
        Espresso.pressBack()
        Espresso.pressBack()
    }

    private fun crawlMatrixServer(isSignUp: Boolean) {
        BaristaClickInteractions.clickOn(R.id.loginServerChoiceMatrixOrg)
        assetMatrixSignInOptions(isSignUp)
        Espresso.pressBack()
    }

    private fun assetMatrixSignInOptions(isSignUp: Boolean) {
        waitUntilViewVisible(ViewMatchers.withId(R.id.loginTitle))
        when (isSignUp) {
            true -> BaristaVisibilityAssertions.assertDisplayed(R.id.loginTitle, "Sign up to matrix.org")
            false -> BaristaVisibilityAssertions.assertDisplayed(R.id.loginTitle, "Connect to matrix.org")
        }
    }

    private fun crawlSignInWithMatrixId() {
        BaristaClickInteractions.clickOn(R.id.loginServerIKnowMyIdSubmit)
        waitUntilViewVisible(ViewMatchers.withId(R.id.loginTitle))
        BaristaVisibilityAssertions.assertDisplayed(R.id.loginTitle, CommonStrings.login_signin_matrix_id_title)
        Espresso.pressBack()
    }
}
