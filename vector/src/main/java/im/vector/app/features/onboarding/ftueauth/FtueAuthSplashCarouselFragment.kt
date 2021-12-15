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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.databinding.FragmentFtueSplashCarouselBinding
import im.vector.app.features.VectorFeatures
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingFlow
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.failure.Failure
import java.net.UnknownHostException
import javax.inject.Inject

@AndroidEntryPoint
class FtueAuthSplashCarouselFragment : AbstractFtueAuthFragment<FragmentFtueSplashCarouselBinding>() {

    @Inject lateinit var vectorPreferences: VectorPreferences
    @Inject lateinit var vectorFeatures: VectorFeatures
    @Inject lateinit var carouselController: SplashCarouselController
    @Inject lateinit var stringProvider: StringProvider

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueSplashCarouselBinding {
        return FragmentFtueSplashCarouselBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        views.splashCarousel.adapter = carouselController.adapter
        TabLayoutMediator(views.carouselIndicator, views.splashCarousel) { _, _ -> }.attach()
        carouselController.setData(SplashCarouselState(
                items = listOf(
                        SplashCarouselState.Item(
                                stringProvider.getString(R.string.ftue_auth_carousel_1_title),
                                stringProvider.getString(R.string.ftue_auth_carousel_1_body),
                                R.drawable.onboarding_carousel_conversations
                        ),
                        SplashCarouselState.Item(
                                stringProvider.getString(R.string.ftue_auth_carousel_2_title),
                                stringProvider.getString(R.string.ftue_auth_carousel_2_body),
                                R.drawable.onboarding_carousel_ems
                        ),
                        SplashCarouselState.Item(
                                stringProvider.getString(R.string.ftue_auth_carousel_3_title),
                                stringProvider.getString(R.string.ftue_auth_carousel_3_body),
                                R.drawable.onboarding_carousel_connect
                        ),
                        SplashCarouselState.Item(
                                stringProvider.getString(R.string.ftue_auth_carousel_4_title),
                                stringProvider.getString(R.string.ftue_auth_carousel_4_body),
                                R.drawable.onboarding_carousel_universal
                        )
                )
        ))

        views.loginSplashSubmit.debouncedClicks { getStarted() }
        views.loginSplashAlreadyHaveAccount.apply {
            isVisible = vectorFeatures.isAlreadyHaveAccountSplashEnabled()
            debouncedClicks { alreadyHaveAnAccount() }
        }

        if (BuildConfig.DEBUG || vectorPreferences.developerMode()) {
//            views.loginSplashVersion.isVisible = true
//            @SuppressLint("SetTextI18n")
//            views.loginSplashVersion.text = "Version : ${BuildConfig.VERSION_NAME}\n" +
//                    "Branch: ${BuildConfig.GIT_BRANCH_NAME}\n" +
//                    "Build: ${BuildConfig.BUILD_NUMBER}"
//            views.loginSplashVersion.debouncedClicks { navigator.openDebug(requireContext()) }
        }
    }

    private fun getStarted() {
        val getStartedFlow = if (vectorFeatures.isAlreadyHaveAccountSplashEnabled()) OnboardingFlow.SignUp else OnboardingFlow.SignInSignUp
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

data class SplashCarouselState(
        val items: List<Item>
) {
    data class Item(
            val title: String,
            val body: String,
            @DrawableRes val image: Int
    )
}

class SplashCarouselController @Inject constructor() : TypedEpoxyController<SplashCarouselState>() {
    override fun buildModels(data: SplashCarouselState) {
        data.items.forEachIndexed { index, item ->
            splashCarouselItem {
                id(index)
                item(item)
            }
        }
    }
}

@EpoxyModelClass(layout = im.vector.app.R.layout.item_splash_carousel)
abstract class SplashCarouselItem : VectorEpoxyModel<SplashCarouselItem.Holder>() {

    @EpoxyAttribute
    lateinit var item: SplashCarouselState.Item

    override fun bind(holder: Holder) {
        holder.image.setImageResource(item.image)
        holder.title.text = item.title
        holder.body.text = item.body
    }

    class Holder : VectorEpoxyHolder() {
        val image by bind<ImageView>(im.vector.app.R.id.carousel_item_image)
        val title by bind<TextView>(im.vector.app.R.id.carousel_item_title)
        val body by bind<TextView>(im.vector.app.R.id.carousel_item_body)
    }
}
