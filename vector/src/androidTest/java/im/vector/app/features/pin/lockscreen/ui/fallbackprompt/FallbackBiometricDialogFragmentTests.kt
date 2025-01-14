/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.ui.fallbackprompt

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.testing.launchFragment
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.airbnb.mvrx.Mavericks
import im.vector.app.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.util.concurrent.CountDownLatch

class FallbackBiometricDialogFragmentTests {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun dismissTriggersOnDismissCallback() {
        val latch = CountDownLatch(1)
        val fragmentScenario = launchFragment<FallbackBiometricDialogFragment>(noArgsBundle())
        fragmentScenario.onFragment { fragment ->
            fragment.onDismiss = { latch.countDown() }
            fragment.dismiss()
        }
        latch.await()
    }

    @Test
    fun argsModifyUI() {
        val latch = CountDownLatch(1)
        val args = FallbackBiometricDialogFragment.Args(
                title = "Title",
                description = "Description",
                cancelActionText = "Cancel text",
        )
        val fragmentScenario = launchFragment<FallbackBiometricDialogFragment>(bundleOf(Mavericks.KEY_ARG to args))
        fragmentScenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<Button>(R.id.cancel_button).text.toString() shouldBeEqualTo args.cancelActionText
            view.findViewById<TextView>(R.id.fingerprint_description).text.toString() shouldBeEqualTo args.description
            (fragment as DialogFragment).requireDialog().window?.attributes?.title shouldBeEqualTo args.title

            latch.countDown()
        }
        latch.await()
    }

    @Test
    fun onSuccessRendersStateAndDismisses() {
        val latch = CountDownLatch(1)
        val authFlow = MutableSharedFlow<Boolean>(replay = 1)
        val fragmentScenario = launchFragment<FallbackBiometricDialogFragment>(noArgsBundle())
        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        fragmentScenario.onFragment { fragment ->
            fragment.onDismiss = { latch.countDown() }
            fragment.authenticationFlow = authFlow
            fragmentScenario.moveToState(Lifecycle.State.RESUMED)
            // Espresso wasn't fast enough to catch this value
            authFlow.tryEmit(true)
            fragment.requireView().statusText() shouldBeEqualTo context.getString(im.vector.lib.ui.styles.R.string.lockscreen_fingerprint_success)
        }
        latch.await()
    }

    @Test
    fun onFailureRendersStateAndResetsItBackAfterDelay() {
        val latch = CountDownLatch(1)
        val authFlow = MutableSharedFlow<Boolean>(replay = 1)
        val fragmentScenario = launchFragment<FallbackBiometricDialogFragment>(noArgsBundle())
        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        fragmentScenario.onFragment { fragment ->
            fragment.authenticationFlow = authFlow
            fragmentScenario.moveToState(Lifecycle.State.RESUMED)
            authFlow.tryEmit(false)
            fragment.requireView().statusText() shouldBeEqualTo context.getString(im.vector.lib.ui.styles.R.string.lockscreen_fingerprint_not_recognized)
            latch.countDown()
        }
        latch.await()
    }

    @Test
    fun onErrorDismissesDialog() {
        val latch = CountDownLatch(1)
        val authChannel = Channel<Boolean>(capacity = 1)
        val fragmentScenario = launchFragment<FallbackBiometricDialogFragment>(noArgsBundle())
        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        fragmentScenario.onFragment { fragment ->
            fragment.onDismiss = { latch.countDown() }
            fragment.authenticationFlow = authChannel.receiveAsFlow()
            fragmentScenario.moveToState(Lifecycle.State.RESUMED)
            authChannel.close(Exception())
        }
        latch.await()
    }

    private fun noArgsBundle() = bundleOf(Mavericks.KEY_ARG to FallbackBiometricDialogFragment.Args())

    private fun View.statusText(): String = findViewById<TextView>(R.id.fingerprint_status).text.toString()
}
