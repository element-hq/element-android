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

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import im.vector.app.core.utils.getMatrixInstance
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.MainActivity
import im.vector.app.features.home.HomeActivity
import im.vector.app.ui.robot.AnalyticsRobot
import im.vector.app.ui.robot.ElementRobot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.amshove.kluent.internal.assertEquals
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.SasTransactionState
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.getRequest
import org.matrix.android.sdk.api.session.crypto.verification.getTransaction
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@LargeTest
class VerifySessionInteractiveTest : VerificationTestBase() {

    var existingSession: Session? = null

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val testScope = CoroutineScope(SupervisorJob())

    @Before
    fun createSessionWithCrossSigning() {
        val matrix = getMatrixInstance()
        val userName = "foobar_${Random.nextLong()}"
        existingSession = createAccountAndSync(matrix, userName, password, true)
        runBlockingTest {
            existingSession!!.cryptoService().crossSigningService()
                    .initializeCrossSigning(
                            object : UserInteractiveAuthInterceptor {
                                override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                                    promise.resume(
                                            UserPasswordAuth(
                                                    user = existingSession!!.myUserId,
                                                    password = "password",
                                                    session = flowResponse.session
                                            )
                                    )
                                }
                            }
                    )
        }
    }

    @Test
    fun checkVerifyPopup() {
        val userId: String = existingSession!!.myUserId

        uiTestBase.login(userId = userId, password = password, homeServerUrl = homeServerUrl)

        val analyticsRobot = AnalyticsRobot()
        analyticsRobot.optOut()

        waitUntilActivityVisible<HomeActivity> {
            waitUntilViewVisible(withId(R.id.roomListContainer))
        }
        val activity = EspressoHelper.getCurrentActivity()!!
        val uiSession = (activity as HomeActivity).activeSessionHolder.getActiveSession()
        withIdlingResource(initialSyncIdlingResource(uiSession)) {
            waitUntilViewVisible(withId(R.id.roomListContainer))
        }

        // THIS IS THE ONLY WAY I FOUND TO CLICK ON ALERTERS... :(
        // Cannot wait for view because of alerter animation? ...
        onView(isRoot())
                .perform(waitForView(withId(com.tapadoo.alerter.R.id.llAlertBackground)))

        Thread.sleep(1000)
        val popup = activity.findViewById<View>(com.tapadoo.alerter.R.id.llAlertBackground)
        activity.runOnUiThread {
            popup.performClick()
        }

        onView(isRoot())
                .perform(waitForView(withId(R.id.bottomSheetFragmentContainer)))

        onView(withText(R.string.verification_verify_identity))
                .check(matches(isDisplayed()))

        // 4S is not setup so passphrase option should be hidden
        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .check(matches(not(hasDescendant(withText(R.string.verification_cannot_access_other_session)))))

        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .check(matches(hasDescendant(withText(R.string.verification_verify_with_another_device))))

        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .check(matches(hasDescendant(withText(R.string.bad_passphrase_key_reset_all_action))))

        val otherRequest = CompletableDeferred<PendingVerificationRequest>()

        testScope.launch {
            existingSession!!.cryptoService().verificationService().requestEventFlow().collect {
                if (it.getRequest() != null) {
                    otherRequest.complete(it.getRequest()!!)
                    return@collect cancel()
                }
            }
        }

        // Send out a self verification request
        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(
                        actionOnItem<RecyclerView.ViewHolder>(
                                hasDescendant(withText(R.string.verification_verify_with_another_device)),
                                click()
                        )
                )

        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .check(matches(hasDescendant(withText(R.string.verification_request_was_sent))))

        val txId = runBlockingTest {
            otherRequest.await().transactionId
        }

        // accept from other session
        runBlockingTest {
            existingSession!!.cryptoService().verificationService().readyPendingVerification(
                    listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW),
                    existingSession!!.myUserId,
                    txId
            )
        }

        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(waitForView(hasDescendant(withText(R.string.verification_scan_self_notice))))
        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(waitForView(hasDescendant(withText(R.string.verification_scan_self_emoji_subtitle))))

        // there should be the QR code also
        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .check(matches(hasDescendant(withId(R.id.itemVerificationQrCodeImage))))

        // proceed with emoji
        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(
                        actionOnItem<RecyclerView.ViewHolder>(
                                hasDescendant(withText(R.string.verification_scan_self_emoji_subtitle)),
                                click()
                        )
                )

        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(waitForView(hasDescendant(withText(R.string.verification_sas_do_not_match))))

        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(waitForView(hasDescendant(withText(R.string.verification_sas_match))))

        // check that the code matches
        val uiCode = runBlockingTest {
            (uiSession.cryptoService()
                    .verificationService()
                    .getExistingTransaction(uiSession.myUserId, txId) as SasVerificationTransaction)
                    .getDecimalCodeRepresentation()!!
        }

        val backgroundCode = runBlockingTest {
            (existingSession!!.cryptoService()
                    .verificationService()
                    .getExistingTransaction(uiSession.myUserId, txId) as SasVerificationTransaction)
                    .getDecimalCodeRepresentation()!!
        }

        assertEquals("SAS code should be equals", backgroundCode, uiCode)

        runBlockingTest {
            (existingSession!!.cryptoService()
                    .verificationService()
                    .getExistingTransaction(uiSession.myUserId, txId) as SasVerificationTransaction)
                    .userHasVerifiedShortCode()
        }

        // Do the same on ui
        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(
                        actionOnItem<RecyclerView.ViewHolder>(
                                hasDescendant(withText(R.string.verification_sas_match)),
                                click()
                        )
                )

        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(waitForView(hasDescendant(withText(R.string.verification_conclusion_ok_notice))))

        // click on done
        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(
                        actionOnItem<RecyclerView.ViewHolder>(
                                hasDescendant(withText(R.string.done)),
                                click()
                        )
                )

        Thread.sleep(1_000)

        // check that current session is actually trusted
        assertTrue("I should be verified",
                runBlockingTest { uiSession.cryptoService().crossSigningService().isCrossSigningVerified() }
        )

        ElementRobot().signout(false)
    }

    fun signout() {
        onView(withId(R.id.groupToolbarAvatarImageView))
                .perform(click())

        onView(withId(R.id.homeDrawerHeaderSettingsView))
                .perform(click())

        onView(withText("General"))
                .perform(click())
    }

    fun verificationStateIdleResource(transactionId: String, checkForState: SasTransactionState, session: Session): IdlingResource {
        val scope = CoroutineScope(SupervisorJob())

        val idle = object : IdlingResource {
            private var callback: IdlingResource.ResourceCallback? = null

            private var currentState: SasTransactionState? = null

            override fun getName() = "verificationSuccessIdle"

            override fun isIdleNow(): Boolean {
                return currentState == checkForState
            }

            override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
                this.callback = callback
            }

            fun update(state: SasTransactionState) {
                currentState = state
                if (state == checkForState) {
                    callback?.onTransitionToIdle()
                    scope.cancel()
                }
            }
        }

        session.cryptoService().verificationService()
                .requestEventFlow()
                .filter {
                    it.transactionId == transactionId
                }
                .onEach {
                    (it.getTransaction() as? SasVerificationTransaction)?.state()?.let {
                        idle.update(it)
                    }
                }.launchIn(scope)
        return idle
    }
}
