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

import android.app.Activity
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.adevinta.android.barista.interaction.BaristaClickInteractions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.espresso.tools.waitUntilViewVisible
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.StringDescription
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.PrivateKeysInfo
import org.matrix.android.sdk.api.session.sync.SyncState
import org.matrix.android.sdk.api.util.Optional
import java.util.concurrent.TimeoutException

object EspressoHelper {
    fun getCurrentActivity(): Activity? {
        var currentActivity: Activity? = null
        getInstrumentation().runOnMainSync {
            currentActivity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).elementAtOrNull(0)
        }
        return currentActivity
    }

    inline fun <reified T : VectorBaseBottomSheetDialogFragment<*>> getBottomSheetDialog(): BottomSheetDialogFragment? {
        return (getCurrentActivity() as? FragmentActivity)
                ?.supportFragmentManager
                ?.fragments
                ?.filterIsInstance<T>()
                ?.firstOrNull()
    }
}

fun withRetry(attempts: Int = 3, action: () -> Unit) {
    runCatching { action() }.onFailure {
        val remainingAttempts = attempts - 1
        if (remainingAttempts <= 0) {
            throw it
        } else {
            Thread.sleep(500)
            withRetry(remainingAttempts, action)
        }
    }
}

fun getString(@StringRes id: Int): String {
    return EspressoHelper.getCurrentActivity()!!.resources.getString(id)
}

fun waitForView(viewMatcher: Matcher<View>, timeout: Long = 10_000, waitForDisplayed: Boolean = true): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return Matchers.any(View::class.java)
        }

        override fun getDescription(): String {
            val matcherDescription = StringDescription()
            viewMatcher.describeTo(matcherDescription)
            return "wait for a specific view <$matcherDescription> to be ${if (waitForDisplayed) "displayed" else "not displayed during $timeout millis."}"
        }

        override fun perform(uiController: UiController, view: View) {
            println("*** waitForView 1 $view")
            uiController.loopMainThreadUntilIdle()
            val startTime = System.currentTimeMillis()
            val endTime = startTime + timeout
            val visibleMatcher = isDisplayed()

            uiController.loopMainThreadForAtLeast(100)

            do {
                println("*** waitForView loop $view end:$endTime current:${System.currentTimeMillis()}")
                val viewVisible = TreeIterables.breadthFirstViewTraversal(view)
                        .any { viewMatcher.matches(it) && visibleMatcher.matches(it) }

                println("*** waitForView loop viewVisible:$viewVisible")
                if (viewVisible == waitForDisplayed) return
                println("*** waitForView loop loopMainThreadForAtLeast...")
                uiController.loopMainThreadForAtLeast(50)
                println("*** waitForView loop ...loopMainThreadForAtLeast")
            } while (System.currentTimeMillis() < endTime)

            println("*** waitForView timeout $view")
            // Timeout happens.
            throw PerformException.Builder()
                    .withActionDescription(this.description)
                    .withViewDescription(HumanReadables.describe(view))
                    .withCause(TimeoutException())
                    .build()
        }
    }
}

fun initialSyncIdlingResource(session: Session): IdlingResource {
    val res = object : IdlingResource, Observer<SyncState> {
        private var callback: IdlingResource.ResourceCallback? = null

        override fun getName() = "InitialSyncIdlingResource for ${session.myUserId}"

        override fun isIdleNow(): Boolean {
            val isIdle = session.hasAlreadySynced()
            return isIdle
        }

        override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
            this.callback = callback
        }

        override fun onChanged(t: SyncState?) {
            val isIdle = session.hasAlreadySynced()
            if (isIdle) {
                callback?.onTransitionToIdle()
                session.getSyncStateLive().removeObserver(this)
            }
        }
    }

    runOnUiThread {
        session.getSyncStateLive().observeForever(res)
    }

    return res
}

fun activityIdlingResource(activityClass: Class<*>): IdlingResource {
    val lifecycleMonitor = ActivityLifecycleMonitorRegistry.getInstance()

    val res = object : IdlingResource, ActivityLifecycleCallback {
        private var callback: IdlingResource.ResourceCallback? = null
        private var resumedActivity: Activity? = null
        private val uniqTS = System.currentTimeMillis()

        override fun getName() = "activityIdlingResource_${activityClass.name}_$uniqTS"

        override fun isIdleNow(): Boolean {
            val activity = resumedActivity ?: ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull {
                activityClass == it.javaClass
            }

            val isIdle = activity != null
            if (isIdle) {
                unregister()
            }
            return isIdle
        }

        override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
            println("*** [$name]  registerIdleTransitionCallback $callback")
            this.callback = callback
        }

        override fun onActivityLifecycleChanged(activity: Activity?, stage: Stage?) {
            if (activityClass == activity?.javaClass) {
                when (stage) {
                    Stage.RESUMED -> {
                        unregister()
                        resumedActivity = activity
                        println("*** [$name]  onActivityLifecycleChanged callback: $callback")
                        callback?.onTransitionToIdle()
                    }
                    else -> {
                        // do nothing, we're blocking until the activity resumes
                    }
                }
            }
        }

        private fun unregister() {
            lifecycleMonitor.removeLifecycleCallback(this)
        }
    }
    lifecycleMonitor.addLifecycleCallback(res)
    return res
}

fun withIdlingResource(idlingResource: IdlingResource, block: (() -> Unit)) {
    println("*** withIdlingResource register")
    IdlingRegistry.getInstance().register(idlingResource)
    block.invoke()
    println("*** withIdlingResource unregister")
    IdlingRegistry.getInstance().unregister(idlingResource)
}

fun allSecretsKnownIdling(session: Session): IdlingResource {
    val res = object : IdlingResource, Observer<Optional<PrivateKeysInfo>> {
        private var callback: IdlingResource.ResourceCallback? = null

        var privateKeysInfo: PrivateKeysInfo? = session.cryptoService().crossSigningService().getCrossSigningPrivateKeys()
        override fun getName() = "AllSecretsKnownIdling_${session.myUserId}"

        override fun isIdleNow(): Boolean {
            println("*** [$name]/isIdleNow  allSecretsKnownIdling ${privateKeysInfo?.allKnown()}")
            return privateKeysInfo?.allKnown() == true
        }

        override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
            this.callback = callback
        }

        override fun onChanged(t: Optional<PrivateKeysInfo>?) {
            println("*** [$name]  allSecretsKnownIdling ${t?.getOrNull()}")
            privateKeysInfo = t?.getOrNull()
            if (t?.getOrNull()?.allKnown() == true) {
                session.cryptoService().crossSigningService().getLiveCrossSigningPrivateKeys().removeObserver(this)
                callback?.onTransitionToIdle()
            }
        }
    }

    runOnUiThread {
        session.cryptoService().crossSigningService().getLiveCrossSigningPrivateKeys().observeForever(res)
    }

    return res
}

fun clickOnAndGoBack(@StringRes name: Int, block: () -> Unit) {
    BaristaClickInteractions.clickOn(name)
    block()
    Espresso.pressBack()
}

fun clickOnSheet(id: Int) {
    Espresso.onView(ViewMatchers.withId(id)).inRoot(RootMatchers.isDialog()).perform(ViewActions.click())
}

inline fun <reified T : VectorBaseBottomSheetDialogFragment<*>> interactWithSheet(
        contentMatcher: Matcher<View>,
        @BottomSheetBehavior.State openState: Int = BottomSheetBehavior.STATE_EXPANDED,
        @BottomSheetBehavior.State exitState: Int = BottomSheetBehavior.STATE_HIDDEN,
        noinline block: () -> Unit = {}
) {
    waitUntilViewVisible(contentMatcher)
    val behaviour = (EspressoHelper.getBottomSheetDialog<T>()!!.dialog as BottomSheetDialog).behavior
    withIdlingResource(BottomSheetResource(behaviour, openState), block)
    withIdlingResource(BottomSheetResource(behaviour, exitState)) {}
}

class BottomSheetResource(
        private val bottomSheetBehavior: BottomSheetBehavior<*>,
        @BottomSheetBehavior.State private val wantedState: Int
) : IdlingResource, BottomSheetBehavior.BottomSheetCallback() {

    private var isIdle: Boolean = false
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    override fun onSlide(bottomSheet: View, slideOffset: Float) {}

    override fun onStateChanged(bottomSheet: View, newState: Int) {
        val wasIdle = isIdle
        isIdle = newState == BottomSheetBehavior.STATE_EXPANDED
        if (!wasIdle && isIdle) {
            bottomSheetBehavior.removeBottomSheetCallback(this)
            resourceCallback?.onTransitionToIdle()
        }
    }

    override fun getName() = "BottomSheet awaiting state: $wantedState"

    override fun isIdleNow() = isIdle

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        resourceCallback = callback

        val state = bottomSheetBehavior.state
        isIdle = state == wantedState
        if (isIdle) {
            resourceCallback!!.onTransitionToIdle()
        } else {
            bottomSheetBehavior.addBottomSheetCallback(this)
        }
    }
}
