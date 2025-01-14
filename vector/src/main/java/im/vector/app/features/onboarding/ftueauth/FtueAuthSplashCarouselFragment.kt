/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.incrementByOneAndWrap
import im.vector.app.core.extensions.setCurrentItem
import im.vector.app.core.resources.BuildMeta
import im.vector.app.databinding.FragmentFtueSplashCarouselBinding
import im.vector.app.features.VectorFeatures
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingFlow
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CAROUSEL_ROTATION_DELAY_MS = 5000L
private const val CAROUSEL_TRANSITION_TIME_MS = 500L

@AndroidEntryPoint
class FtueAuthSplashCarouselFragment :
        AbstractFtueAuthFragment<FragmentFtueSplashCarouselBinding>() {

    @Inject lateinit var vectorPreferences: VectorPreferences
    @Inject lateinit var vectorFeatures: VectorFeatures
    @Inject lateinit var carouselController: SplashCarouselController
    @Inject lateinit var carouselStateFactory: SplashCarouselStateFactory
    @Inject lateinit var buildMeta: BuildMeta

    private var tabLayoutMediator: TabLayoutMediator? = null

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueSplashCarouselBinding {
        return FragmentFtueSplashCarouselBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    override fun onDestroyView() {
        tabLayoutMediator?.detach()
        tabLayoutMediator = null
        views.splashCarousel.adapter = null
        super.onDestroyView()
    }

    private fun setupViews() {
        val carouselAdapter = carouselController.adapter
        views.splashCarousel.adapter = carouselAdapter
        tabLayoutMediator = TabLayoutMediator(views.carouselIndicator, views.splashCarousel) { _, _ -> }
                .also { it.attach() }

        carouselController.setData(carouselStateFactory.create())

        val isAlreadyHaveAccountEnabled = vectorFeatures.isOnboardingAlreadyHaveAccountSplashEnabled()
        views.loginSplashSubmit.apply {
            setText(if (isAlreadyHaveAccountEnabled) CommonStrings.login_splash_create_account else CommonStrings.login_splash_submit)
            debouncedClicks { splashSubmit(isAlreadyHaveAccountEnabled) }
        }
        views.loginSplashAlreadyHaveAccount.apply {
            isVisible = isAlreadyHaveAccountEnabled
            debouncedClicks { alreadyHaveAnAccount() }
        }

        if (buildMeta.isDebug || vectorPreferences.developerMode()) {
            views.loginSplashVersion.isVisible = true
            @SuppressLint("SetTextI18n")
            views.loginSplashVersion.text = "Version : ${buildMeta.versionName}\n" +
                    "Branch: ${buildMeta.gitBranchName} ${buildMeta.gitRevision}"
            views.loginSplashVersion.debouncedClicks { navigator.openDebug(requireContext()) }
        }
        views.splashCarousel.registerAutomaticUntilInteractionTransitions()
    }

    private fun ViewPager2.registerAutomaticUntilInteractionTransitions() {
        var scheduledTransition: Job? = null
        val pageChangingCallback = object : ViewPager2.OnPageChangeCallback() {
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
        }
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                registerOnPageChangeCallback(pageChangingCallback)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                unregisterOnPageChangeCallback(pageChangingCallback)
            }
        })
    }

    private fun ViewPager2.scheduleCarouselTransition(): Job {
        val itemCount = adapter?.itemCount ?: throw IllegalStateException("An adapter must be set")
        return viewLifecycleOwner.lifecycleScope.launch {
            delay(CAROUSEL_ROTATION_DELAY_MS)
            setCurrentItem(currentItem.incrementByOneAndWrap(max = itemCount - 1), duration = CAROUSEL_TRANSITION_TIME_MS)
        }
    }

    private fun splashSubmit(isAlreadyHaveAccountEnabled: Boolean) {
        val getStartedFlow = if (isAlreadyHaveAccountEnabled) OnboardingFlow.SignUp else OnboardingFlow.SignInSignUp
        viewModel.handle(OnboardingAction.SplashAction.OnGetStarted(onboardingFlow = getStartedFlow))
    }

    private fun alreadyHaveAnAccount() {
        viewModel.handle(OnboardingAction.SplashAction.OnIAlreadyHaveAnAccount(onboardingFlow = OnboardingFlow.SignIn))
    }

    override fun resetViewModel() {
        // Nothing to do
    }
}
