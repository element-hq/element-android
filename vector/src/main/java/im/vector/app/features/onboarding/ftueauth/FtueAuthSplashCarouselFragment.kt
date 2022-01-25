/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.onboarding.ftueauth

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.core.extensions.incrementByOneAndWrap
import im.vector.app.core.extensions.setCurrentItem
import im.vector.app.databinding.FragmentFtueSplashCarouselBinding
import im.vector.app.features.VectorFeatures
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingFlow
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.failure.Failure
import java.net.UnknownHostException
import javax.inject.Inject

private const val CAROUSEL_ROTATION_DELAY_MS = 5000L
private const val CAROUSEL_TRANSITION_TIME_MS = 500L

class FtueAuthSplashCarouselFragment @Inject constructor(
        private val vectorPreferences: VectorPreferences,
        private val vectorFeatures: VectorFeatures,
        private val carouselController: SplashCarouselController,
        private val carouselStateFactory: SplashCarouselStateFactory
) : AbstractFtueAuthFragment<FragmentFtueSplashCarouselBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueSplashCarouselBinding {
        return FragmentFtueSplashCarouselBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        val carouselAdapter = carouselController.adapter
        views.splashCarousel.adapter = carouselAdapter
        TabLayoutMediator(views.carouselIndicator, views.splashCarousel) { _, _ -> }.attach()
        carouselController.setData(carouselStateFactory.create())

        val isAlreadyHaveAccountEnabled = vectorFeatures.isOnboardingAlreadyHaveAccountSplashEnabled()
        views.loginSplashSubmit.apply {
            setText(if (isAlreadyHaveAccountEnabled) R.string.login_splash_create_account else R.string.login_splash_submit)
            debouncedClicks { splashSubmit(isAlreadyHaveAccountEnabled) }
        }
        views.loginSplashAlreadyHaveAccount.apply {
            isVisible = isAlreadyHaveAccountEnabled
            debouncedClicks { alreadyHaveAnAccount() }
        }

        if (BuildConfig.DEBUG || vectorPreferences.developerMode()) {
            views.loginSplashVersion.isVisible = true
            @SuppressLint("SetTextI18n")
            views.loginSplashVersion.text = "Version : ${BuildConfig.VERSION_NAME}#${BuildConfig.BUILD_NUMBER}\n" +
                    "Branch: ${BuildConfig.GIT_BRANCH_NAME}"
            views.loginSplashVersion.debouncedClicks { navigator.openDebug(requireContext()) }
        }
        views.splashCarousel.registerAutomaticUntilInteractionTransitions()
    }

    private fun ViewPager2.registerAutomaticUntilInteractionTransitions() {
        var scheduledTransition: Job? = null
        registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private var hasUserManuallyInteractedWithCarousel: Boolean = false

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                hasUserManuallyInteractedWithCarousel = !isFakeDragging
            }

            override fun onPageSelected(position: Int) {
                scheduledTransition?.cancel()
                // only schedule automatic transitions whilst the user has not interacted with the carousel
                if (!hasUserManuallyInteractedWithCarousel) {
                    scheduledTransition = scheduleCarouselTransition()
                }
            }
        })
    }

    private fun ViewPager2.scheduleCarouselTransition(): Job {
        val itemCount = adapter?.itemCount ?: throw IllegalStateException("An adapter must be set")
        return lifecycleScope.launch {
            delay(CAROUSEL_ROTATION_DELAY_MS)
            setCurrentItem(currentItem.incrementByOneAndWrap(max = itemCount - 1), duration = CAROUSEL_TRANSITION_TIME_MS)
        }
    }

    private fun splashSubmit(isAlreadyHaveAccountEnabled: Boolean) {
        val getStartedFlow = if (isAlreadyHaveAccountEnabled) OnboardingFlow.SignUp else OnboardingFlow.SignInSignUp
        viewModel.handle(OnboardingAction.OnGetStarted(resetLoginConfig = false, onboardingFlow = getStartedFlow))
    }

    private fun alreadyHaveAnAccount() {
        viewModel.handle(OnboardingAction.OnIAlreadyHaveAnAccount(resetLoginConfig = false, onboardingFlow = OnboardingFlow.SignIn))
    }

    override fun resetViewModel() {
        // Nothing to do
    }

    override fun onError(throwable: Throwable) {
        if (throwable is Failure.NetworkConnection &&
                throwable.ioException is UnknownHostException) {
            // Invalid homeserver from URL config
            val url = viewModel.getInitialHomeServerUrl().orEmpty()
            MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.dialog_title_error)
                    .setMessage(getString(R.string.login_error_homeserver_from_url_not_found, url))
                    .setPositiveButton(R.string.login_error_homeserver_from_url_not_found_enter_manual) { _, _ ->
                        val flow = withState(viewModel) { it.onboardingFlow } ?: OnboardingFlow.SignInSignUp
                        viewModel.handle(OnboardingAction.OnGetStarted(resetLoginConfig = true, flow))
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
        } else {
            super.onError(throwable)
        }
    }
}
