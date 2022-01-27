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

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import im.vector.app.R
import im.vector.app.core.extensions.getResTintedDrawable
import im.vector.app.core.extensions.getTintedDrawable
import im.vector.app.core.extensions.setLeftDrawable
import im.vector.app.core.extensions.setTextWithColoredPart
import im.vector.app.databinding.FragmentFtueAuthUseCaseBinding
import im.vector.app.features.login.ServerType
import im.vector.app.features.onboarding.FtueUseCase
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.themes.ThemeProvider
import javax.inject.Inject

private const val DARK_MODE_ICON_BACKGROUND_ALPHA = 0.30f
private const val LIGHT_MODE_ICON_BACKGROUND_ALPHA = 0.15f

class FtueAuthUseCaseFragment @Inject constructor(
        private val themeProvider: ThemeProvider
) : AbstractFtueAuthFragment<FragmentFtueAuthUseCaseBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueAuthUseCaseBinding {
        return FragmentFtueAuthUseCaseBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        views.useCaseOptionOne.renderUseCase(
                useCase = FtueUseCase.FRIENDS_FAMILY,
                label = R.string.ftue_auth_use_case_option_one,
                icon = R.drawable.ic_use_case_friends,
                tint = R.color.palette_grape
        )
        views.useCaseOptionTwo.renderUseCase(
                useCase = FtueUseCase.TEAMS,
                label = R.string.ftue_auth_use_case_option_two,
                icon = R.drawable.ic_use_case_teams,
                tint = R.color.palette_element_green
        )
        views.useCaseOptionThree.renderUseCase(
                useCase = FtueUseCase.COMMUNITIES,
                label = R.string.ftue_auth_use_case_option_three,
                icon = R.drawable.ic_use_case_communities,
                tint = R.color.palette_azure
        )

        views.useCaseSkip.setTextWithColoredPart(
                fullTextRes = R.string.ftue_auth_use_case_skip,
                coloredTextRes = R.string.ftue_auth_use_case_skip_partial,
                underline = false,
                colorAttribute = R.attr.colorAccent,
                onClick = { viewModel.handle(OnboardingAction.UpdateUseCase(FtueUseCase.SKIP)) }
        )

        views.useCaseConnectToServer.setOnClickListener {
            viewModel.handle(OnboardingAction.UpdateServerType(ServerType.Other))
        }
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetUseCase)
    }

    private fun TextView.renderUseCase(useCase: FtueUseCase, @StringRes label: Int, @DrawableRes icon: Int, @ColorRes tint: Int) {
        setLeftDrawable(createIcon(tint, icon, isLightMode = themeProvider.isLightTheme()))
        setText(label)
        debouncedClicks {
            viewModel.handle(OnboardingAction.UpdateUseCase(useCase))
        }
    }

    private fun createIcon(@ColorRes tint: Int, icon: Int, isLightMode: Boolean): Drawable {
        val context = requireContext()
        val alpha = when (isLightMode) {
            true  -> LIGHT_MODE_ICON_BACKGROUND_ALPHA
            false -> DARK_MODE_ICON_BACKGROUND_ALPHA
        }
        val iconBackground = context.getResTintedDrawable(R.drawable.bg_feature_icon, tint, alpha = alpha)
        val whiteLayer = context.getTintedDrawable(R.drawable.bg_feature_icon, Color.WHITE)
        return LayerDrawable(arrayOf(whiteLayer, iconBackground, ContextCompat.getDrawable(context, icon)))
    }
}
