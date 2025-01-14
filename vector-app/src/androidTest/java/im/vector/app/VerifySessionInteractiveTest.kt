/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app

import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import im.vector.app.core.utils.getMatrixInstance
import im.vector.app.features.MainActivity
import im.vector.app.ui.robot.ElementRobot
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
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
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@LargeTest
@Ignore
class VerifySessionInteractiveTest : VerificationTestBase() {

    var existingSession: Session? = null

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

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

    @After
    fun cleanUp() {
        runTest {
            existingSession?.signOutService()?.signOut(true)
        }
        val app = EspressoHelper.getCurrentActivity()!!.application as VectorApplication
        while (app.authenticationService.getLastAuthenticatedSession() != null) {
            val session = app.authenticationService.getLastAuthenticatedSession()!!
            runTest {
                session.signOutService().signOut(true)
            }
        }

        val activity = EspressoHelper.getCurrentActivity()!!
        val editor = PreferenceManager.getDefaultSharedPreferences(activity).edit()
        editor.clear()
        editor.commit()
    }

    @Test
    fun checkVerifyPopup() {
        val userId: String = existingSession!!.myUserId

        val uiSession = loginAndClickVerifyToast(userId)

        val otherRequest = deferredRequestUntil(existingSession!!) {
            true
        }

        // Send out a self verification request
        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(
                        actionOnItem<RecyclerView.ViewHolder>(
                                hasDescendant(withText(CommonStrings.verification_verify_with_another_device)),
                                click()
                        )
                )

        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .check(matches(hasDescendant(withText(CommonStrings.verification_request_was_sent))))

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
                .perform(waitForView(hasDescendant(withText(CommonStrings.verification_scan_self_notice))))
        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(waitForView(hasDescendant(withText(CommonStrings.verification_scan_self_emoji_subtitle))))

        // there should be the QR code also
        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .check(matches(hasDescendant(withId(R.id.itemVerificationQrCodeImage))))

        // proceed with emoji
        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(
                        actionOnItem<RecyclerView.ViewHolder>(
                                hasDescendant(withText(CommonStrings.verification_scan_self_emoji_subtitle)),
                                click()
                        )
                )

        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(waitForView(hasDescendant(withText(CommonStrings.verification_sas_do_not_match))))

        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(waitForView(hasDescendant(withText(CommonStrings.verification_sas_match))))

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
                                hasDescendant(withText(CommonStrings.verification_sas_match)),
                                click()
                        )
                )

        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(waitForView(hasDescendant(withText(CommonStrings.verification_conclusion_ok_notice))))

        // click on done
        onView(withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(
                        actionOnItem<RecyclerView.ViewHolder>(
                                hasDescendant(withText(CommonStrings.done)),
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
}
