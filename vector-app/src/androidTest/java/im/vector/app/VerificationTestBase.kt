/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app

import android.net.Uri
import android.view.View
import androidx.lifecycle.Observer
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.home.HomeActivity
import im.vector.app.ui.robot.AnalyticsRobot
import im.vector.app.ui.robot.OnboardingRobot
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.registration.RegistrationResult
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.getRequest
import org.matrix.android.sdk.api.session.sync.SyncState
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

abstract class VerificationTestBase {

    val password = "password"
    val homeServerUrl: String = "http://10.0.2.2:8080"

    protected val uiTestBase = OnboardingRobot()

    protected val testScope = CoroutineScope(SupervisorJob())

    fun createAccountAndSync(
            matrix: Matrix,
            userName: String,
            password: String,
            withInitialSync: Boolean
    ): Session {
        val hs = createHomeServerConfig()

        runBlockingTest {
            matrix.authenticationService().getLoginFlow(hs)
        }

        runBlockingTest {
            matrix.authenticationService()
                    .getRegistrationWizard()
                    .createAccount(userName, password, null)
        }

        // Perform dummy step
        val registrationResult = runBlockingTest {
            matrix.authenticationService()
                    .getRegistrationWizard()
                    .dummy()
        }

        Assert.assertTrue(registrationResult is RegistrationResult.Success)
        val session = (registrationResult as RegistrationResult.Success).session
        if (withInitialSync) {
            syncSession(session)
        }

        return session
    }

    private fun createHomeServerConfig(): HomeServerConnectionConfig {
        return HomeServerConnectionConfig.Builder()
                .withHomeServerUri(Uri.parse(homeServerUrl))
                .build()
    }

    protected fun <T> runBlockingTest(timeout: Long = 20_000, block: suspend () -> T): T {
        return runBlocking {
            withTimeout(timeout) {
                block()
            }
        }
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun syncSession(session: Session) {
        val lock = CountDownLatch(1)

        GlobalScope.launch(Dispatchers.Main) {
            session.open()
            session.syncService().startSync(true)
        }

        val syncLiveData = runBlocking(Dispatchers.Main) {
            session.syncService().getSyncStateLive()
        }
        val syncObserver = object : Observer<SyncState> {
            override fun onChanged(value: SyncState) {
                if (session.syncService().hasAlreadySynced()) {
                    lock.countDown()
                    syncLiveData.removeObserver(this)
                }
            }
        }
        GlobalScope.launch(Dispatchers.Main) { syncLiveData.observeForever(syncObserver) }

        lock.await(20_000, TimeUnit.MILLISECONDS)
    }

    protected fun loginAndClickVerifyToast(userId: String): Session {
        uiTestBase.login(userId = userId, password = password, homeServerUrl = homeServerUrl)

        tryOrNull {
            val analyticsRobot = AnalyticsRobot()
            analyticsRobot.optOut()
        }

        waitUntilActivityVisible<HomeActivity> {
            waitUntilViewVisible(ViewMatchers.withId(R.id.roomListContainer))
        }
        val activity = EspressoHelper.getCurrentActivity()!!
        val uiSession = (activity as HomeActivity).activeSessionHolder.getActiveSession()
        withIdlingResource(initialSyncIdlingResource(uiSession)) {
            waitUntilViewVisible(ViewMatchers.withId(R.id.roomListContainer))
        }

        // THIS IS THE ONLY WAY I FOUND TO CLICK ON ALERTERS... :(
        // Cannot wait for view because of alerter animation? ...
        Espresso.onView(ViewMatchers.isRoot())
                .perform(waitForView(ViewMatchers.withId(com.tapadoo.alerter.R.id.llAlertBackground)))

        Thread.sleep(1000)
        val popup = activity.findViewById<View>(com.tapadoo.alerter.R.id.llAlertBackground)
        activity.runOnUiThread {
            popup.performClick()
        }

        Espresso.onView(ViewMatchers.isRoot())
                .perform(waitForView(ViewMatchers.withId(R.id.bottomSheetFragmentContainer)))

        Espresso.onView(ViewMatchers.withText(CommonStrings.verification_verify_identity))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // 4S is not setup so passphrase option should be hidden
        Espresso.onView(ViewMatchers.withId(R.id.bottomSheetVerificationRecyclerView))
                .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.hasDescendant(ViewMatchers.withText(CommonStrings.verification_cannot_access_other_session)))))

        Espresso.onView(ViewMatchers.withId(R.id.bottomSheetVerificationRecyclerView))
                .check(ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText(CommonStrings.verification_verify_with_another_device))))

        Espresso.onView(ViewMatchers.withId(R.id.bottomSheetVerificationRecyclerView))
                .check(ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText(CommonStrings.bad_passphrase_key_reset_all_action))))

        return uiSession
    }

    protected fun deferredRequestUntil(session: Session, block: ((PendingVerificationRequest) -> Boolean)): CompletableDeferred<PendingVerificationRequest> {
        val completableDeferred = CompletableDeferred<PendingVerificationRequest>()

        testScope.launch {
            session.cryptoService().verificationService().requestEventFlow().collect {
                val request = it.getRequest()
                if (request != null && block(request)) {
                    completableDeferred.complete(request)
                    return@collect cancel()
                }
            }
        }

        return completableDeferred
    }
}
