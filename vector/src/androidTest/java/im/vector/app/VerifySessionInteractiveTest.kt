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

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
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
import im.vector.app.features.MainActivity
import im.vector.app.features.home.HomeActivity
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@RunWith(AndroidJUnit4::class)
@LargeTest
class VerifySessionInteractiveTest : VerificationTestBase() {

    var existingSession: Session? = null

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun createSessionWithCrossSigning() {
        val matrix = getMatrixInstance()
        val userName = "foobar_${System.currentTimeMillis()}"
        existingSession = createAccountAndSync(matrix, userName, password, true)
        doSync<Unit> {
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
                            }, it)
        }
    }

    @Test
    fun checkVerifyPopup() {
        val userId: String = existingSession!!.myUserId

        uiTestBase.login(userId = userId, password = password, homeServerUrl = homeServerUrl)

        // Thread.sleep(6000)
        withIdlingResource(activityIdlingResource(HomeActivity::class.java)) {
            onView(withId(R.id.roomListContainer))
                    .check(matches(isDisplayed()))
                    .perform(closeSoftKeyboard())
        }

        val activity = EspressoHelper.getCurrentActivity()!!
        val uiSession = (activity as HomeActivity).activeSessionHolder.getActiveSession()

        withIdlingResource(initialSyncIdlingResource(uiSession)) {
            onView(withId(R.id.roomListContainer))
                    .check(matches(isDisplayed()))
        }

        // THIS IS THE ONLY WAY I FOUND TO CLICK ON ALERTERS... :(
        // Cannot wait for view because of alerter animation? ...
        onView(isRoot())
                .perform(waitForView(withId(com.tapadoo.alerter.R.id.llAlertBackground)))
//        Thread.sleep(1000)
//        onView(withId(com.tapadoo.alerter.R.id.llAlertBackground))
//                .perform(click())
        Thread.sleep(1000)
        val popup = activity.findViewById<View>(com.tapadoo.alerter.R.id.llAlertBackground)
        activity.runOnUiThread {
            popup.performClick()
        }

        onView(isRoot())
                .perform(waitForView(withId(R.id.bottomSheetFragmentContainer)))
//                .check()
//        onView(withId(R.id.bottomSheetFragmentContainer))
//                .check(matches(isDisplayed()))

//        onView(isRoot()).perform(SleepViewAction.sleep(2000))

        onView(withText(R.string.use_latest_app))
                .check(matches(isDisplayed()))

        // 4S is not setup so passphrase option should be hidden
        onView(withId(R.id.bottomSheetFragmentContainer))
                .check(matches(not(hasDescendant(withText(R.string.verification_cannot_access_other_session)))))

        val request = existingSession!!.cryptoService().verificationService().requestKeyVerification(
                listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW),
                existingSession!!.myUserId,
                listOf(uiSession.sessionParams.deviceId!!)
        )

        val transactionId = request.transactionId!!
        val sasReadyIdle = verificationStateIdleResource(transactionId, VerificationTxState.ShortCodeReady, uiSession)
        val otherSessionSasReadyIdle = verificationStateIdleResource(transactionId, VerificationTxState.ShortCodeReady, existingSession!!)

        onView(isRoot()).perform(SleepViewAction.sleep(1000))

        // Assert QR code option is there and available
        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .check(matches(hasDescendant(withText(R.string.verification_scan_their_code))))

        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .check(matches(hasDescendant(withId(R.id.itemVerificationQrCodeImage))))

        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(
                        actionOnItem<RecyclerView.ViewHolder>(
                                hasDescendant(withText(R.string.verification_scan_emoji_title)),
                                click()
                        )
                )

        val firstSessionTr = existingSession!!.cryptoService().verificationService().getExistingTransaction(
                existingSession!!.myUserId,
                transactionId
        ) as SasVerificationTransaction

        IdlingRegistry.getInstance().register(sasReadyIdle)
        IdlingRegistry.getInstance().register(otherSessionSasReadyIdle)
        onView(isRoot()).perform(SleepViewAction.sleep(300))
        // will only execute when Idle is ready
        val expectedEmojis = firstSessionTr.getEmojiCodeRepresentation()
        val targets = listOf(R.id.emoji0, R.id.emoji1, R.id.emoji2, R.id.emoji3, R.id.emoji4, R.id.emoji5, R.id.emoji6)
        targets.forEachIndexed { index, res ->
            onView(withId(res))
                    .check(
                            matches(hasDescendant(withText(expectedEmojis[index].nameResId)))
                    )
        }

        IdlingRegistry.getInstance().unregister(sasReadyIdle)
        IdlingRegistry.getInstance().unregister(otherSessionSasReadyIdle)

        val verificationSuccessIdle =
                verificationStateIdleResource(transactionId, VerificationTxState.Verified, uiSession)

        // CLICK ON THEY MATCH

        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(
                        actionOnItem<RecyclerView.ViewHolder>(
                                hasDescendant(withText(R.string.verification_sas_match)),
                                click()
                        )
                )

        firstSessionTr.userHasVerifiedShortCode()

        onView(isRoot()).perform(SleepViewAction.sleep(1000))

        withIdlingResource(verificationSuccessIdle) {
            onView(withId(R.id.bottomSheetVerificationRecyclerView))
                    .check(
                            matches(hasDescendant(withText(R.string.verification_conclusion_ok_self_notice)))
                    )
        }

        // Wait a bit before done (to delay a bit sending of secrets to let other have time
        // to mark as verified :/
        Thread.sleep(5_000)
        // Click on done
        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(
                        actionOnItem<RecyclerView.ViewHolder>(
                                hasDescendant(withText(R.string.done)),
                                click()
                        )
                )

        // Wait until local secrets are known (gossip)
        withIdlingResource(allSecretsKnownIdling(uiSession)) {
            onView(withId(R.id.groupToolbarAvatarImageView))
                    .perform(click())
        }
    }

    fun signout() {
        onView(withId(R.id.groupToolbarAvatarImageView))
                .perform(click())

        onView(withId(R.id.homeDrawerHeaderSettingsView))
                .perform(click())

        onView(withText("General"))
                .perform(click())
    }

    fun verificationStateIdleResource(transactionId: String, checkForState: VerificationTxState, session: Session): IdlingResource {
        val idle = object : IdlingResource, VerificationService.Listener {
            private var callback: IdlingResource.ResourceCallback? = null

            private var currentState: VerificationTxState? = null

            override fun getName() = "verificationSuccessIdle"

            override fun isIdleNow(): Boolean {
                return currentState == checkForState
            }

            override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
                this.callback = callback
            }

            fun update(state: VerificationTxState) {
                currentState = state
                if (state == checkForState) {
                    session.cryptoService().verificationService().removeListener(this)
                    callback?.onTransitionToIdle()
                }
            }

            /**
             * Called when a transaction is created, either by the user or initiated by the other user.
             */
            override fun transactionCreated(tx: VerificationTransaction) {
                if (tx.transactionId == transactionId) update(tx.state)
            }

            /**
             * Called when a transaction is updated. You may be interested to track the state of the VerificationTransaction.
             */
            override fun transactionUpdated(tx: VerificationTransaction) {
                if (tx.transactionId == transactionId) update(tx.state)
            }
        }

        session.cryptoService().verificationService().addListener(idle)
        return idle
    }

    object UITestVerificationUtils
}
