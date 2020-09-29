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

import android.net.Uri
import androidx.lifecycle.Observer
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.LoginFlowResult
import org.matrix.android.sdk.api.auth.registration.RegistrationResult
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.sync.SyncState
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

abstract class VerificationTestBase {

    val password = "password"
    val homeServerUrl: String = "http://10.0.2.2:8080"

    fun doLogin(homeServerUrl: String, userId: String, password: String) {
        Espresso.onView(ViewMatchers.withId(R.id.loginSplashSubmit))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .check(ViewAssertions.matches(ViewMatchers.withText(R.string.login_splash_submit)))

        Espresso.onView(ViewMatchers.withId(R.id.loginSplashSubmit))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.loginServerTitle))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .check(ViewAssertions.matches(ViewMatchers.withText(R.string.login_server_title)))

        // Chose custom server
        Espresso.onView(ViewMatchers.withId(R.id.loginServerChoiceOther))
                .perform(ViewActions.click())

        // Enter local synapse
        Espresso.onView((ViewMatchers.withId(R.id.loginServerUrlFormHomeServerUrl)))
                .perform(ViewActions.typeText(homeServerUrl))

        Espresso.onView(ViewMatchers.withId(R.id.loginServerUrlFormSubmit))
                .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
                .perform(ViewActions.closeSoftKeyboard(), ViewActions.click())

        // Click on the signin button
        Espresso.onView(ViewMatchers.withId(R.id.loginSignupSigninSignIn))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())

        // Ensure password flow supported
        Espresso.onView(ViewMatchers.withId(R.id.loginField))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.passwordField))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView((ViewMatchers.withId(R.id.loginField)))
                .perform(ViewActions.typeText(userId))
        Espresso.onView(ViewMatchers.withId(R.id.loginSubmit))
                .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isEnabled())))

        Espresso.onView((ViewMatchers.withId(R.id.passwordField)))
                .perform(ViewActions.closeSoftKeyboard(), ViewActions.typeText(password))

        Espresso.onView(ViewMatchers.withId(R.id.loginSubmit))
                .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
                .perform(ViewActions.closeSoftKeyboard(), ViewActions.click())
    }

    private fun createAccount(userId: String = "UiAutoTest", password: String = "password", homeServerUrl: String = "http://10.0.2.2:8080") {
        Espresso.onView(ViewMatchers.withId(R.id.loginSplashSubmit))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .check(ViewAssertions.matches(ViewMatchers.withText(R.string.login_splash_submit)))

        Espresso.onView(ViewMatchers.withId(R.id.loginSplashSubmit))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.loginServerTitle))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .check(ViewAssertions.matches(ViewMatchers.withText(R.string.login_server_title)))

        // Chose custom server
        Espresso.onView(ViewMatchers.withId(R.id.loginServerChoiceOther))
                .perform(ViewActions.click())

        // Enter local synapse
        Espresso.onView((ViewMatchers.withId(R.id.loginServerUrlFormHomeServerUrl)))
                .perform(ViewActions.typeText(homeServerUrl))

        Espresso.onView(ViewMatchers.withId(R.id.loginServerUrlFormSubmit))
                .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
                .perform(ViewActions.closeSoftKeyboard(), ViewActions.click())

        // Click on the signup button
        Espresso.onView(ViewMatchers.withId(R.id.loginSignupSigninSubmit))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())

        // Ensure password flow supported
        Espresso.onView(ViewMatchers.withId(R.id.loginField))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.passwordField))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView((ViewMatchers.withId(R.id.loginField)))
                .perform(ViewActions.typeText(userId))
        Espresso.onView(ViewMatchers.withId(R.id.loginSubmit))
                .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isEnabled())))

        Espresso.onView((ViewMatchers.withId(R.id.passwordField)))
                .perform(ViewActions.typeText(password))

        Espresso.onView(ViewMatchers.withId(R.id.loginSubmit))
                .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
                .perform(ViewActions.closeSoftKeyboard(), ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.homeDrawerFragmentContainer))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    fun createAccountAndSync(matrix: Matrix, userName: String,
                                     password: String,
                                     withInitialSync: Boolean): Session {
        val hs = createHomeServerConfig()

        doSync<LoginFlowResult> {
            matrix.authenticationService()
                    .getLoginFlow(hs, it)
        }

        doSync<RegistrationResult> {
            matrix.authenticationService()
                    .getRegistrationWizard()
                    .createAccount(userName, password, null, it)
        }

        // Perform dummy step
        val registrationResult = doSync<RegistrationResult> {
            matrix.authenticationService()
                    .getRegistrationWizard()
                    .dummy(it)
        }

        Assert.assertTrue(registrationResult is RegistrationResult.Success)
        val session = (registrationResult as RegistrationResult.Success).session
        if (withInitialSync) {
            syncSession(session)
        }

        return session
    }

    fun createHomeServerConfig(): HomeServerConnectionConfig {
        return HomeServerConnectionConfig.Builder()
                .withHomeServerUri(Uri.parse(homeServerUrl))
                .build()
    }

    // Transform a method with a MatrixCallback to a synchronous method
    inline fun <reified T> doSync(block: (MatrixCallback<T>) -> Unit): T {
        val lock = CountDownLatch(1)
        var result: T? = null

        val callback = object : TestMatrixCallback<T>(lock) {
            override fun onSuccess(data: T) {
                result = data
                super.onSuccess(data)
            }
        }

        block.invoke(callback)

        lock.await(20_000, TimeUnit.MILLISECONDS)

        Assert.assertNotNull(result)
        return result!!
    }

    fun syncSession(session: Session) {
        val lock = CountDownLatch(1)

        GlobalScope.launch(Dispatchers.Main) { session.open() }

        session.startSync(true)

        val syncLiveData = runBlocking(Dispatchers.Main) {
            session.getSyncStateLive()
        }
        val syncObserver = object : Observer<SyncState> {
            override fun onChanged(t: SyncState?) {
                if (session.hasAlreadySynced()) {
                    lock.countDown()
                    syncLiveData.removeObserver(this)
                }
            }
        }
        GlobalScope.launch(Dispatchers.Main) { syncLiveData.observeForever(syncObserver) }

        lock.await(20_000, TimeUnit.MILLISECONDS)
    }
}
