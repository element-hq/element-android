/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import im.vector.app.core.utils.getMatrixInstance
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.MainActivity
import im.vector.app.features.home.HomeActivity
import im.vector.app.ui.robot.ElementRobot
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.test.runTest
import org.junit.After
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
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@LargeTest
@Ignore
class VerifySessionNavigationTest : VerificationTestBase() {

    var existingSession: Session? = null

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun createSessionWithCrossSigning() {
        val matrix = getMatrixInstance()
        val userName = "foobar_${Random.nextLong()}"
        existingSession = createAccountAndSync(matrix, userName, password, true)
        runTest {
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
    }

    @Test
    fun testStartThenCancelRequest() {
        val userId: String = existingSession!!.myUserId

        loginAndClickVerifyToast(userId)

        val otherRequest = deferredRequestUntil(existingSession!!) {
            true
        }

        // Send out a self verification request
        Espresso.onView(ViewMatchers.withId(R.id.bottomSheetVerificationRecyclerView))
                .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                ViewMatchers.hasDescendant(ViewMatchers.withText(CommonStrings.verification_verify_with_another_device)),
                                ViewActions.click()
                        )
                )

        Espresso.onView(ViewMatchers.withId(R.id.bottomSheetVerificationRecyclerView))
                .check(ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText(CommonStrings.verification_request_was_sent))))

        val txId = runBlockingTest {
            otherRequest.await().transactionId
        }

        // if we press back it should cancel

        val otherGetCancelledRequest = deferredRequestUntil(existingSession!!) {
            it.transactionId == txId && it.state == EVerificationState.Cancelled
        }

        Espresso.pressBack()

        // Should go back to main verification options
        Espresso.onView(ViewMatchers.isRoot())
                .perform(waitForView(ViewMatchers.withId(R.id.bottomSheetFragmentContainer)))

        Espresso.onView(ViewMatchers.withId(R.id.bottomSheetVerificationRecyclerView))
                .check(ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText(CommonStrings.verification_verify_with_another_device))))

        runBlockingTest {
            otherGetCancelledRequest.await()
        }

        Espresso.pressBack()
        waitUntilActivityVisible<HomeActivity> {
            waitUntilViewVisible(ViewMatchers.withId(R.id.roomListContainer))
        }

        ElementRobot().signout(false)
    }
}
