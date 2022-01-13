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
import android.widget.TextView
import androidx.annotation.StringRes
import im.vector.app.R
import im.vector.app.core.extensions.setTextWithColoredPart
import im.vector.app.databinding.FragmentFtueAuthUseCaseBinding
import im.vector.app.features.login.ServerType
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.FtueUseCase
import javax.inject.Inject

class FtueAuthUseCaseFragment @Inject constructor() : AbstractFtueAuthFragment<FragmentFtueAuthUseCaseBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueAuthUseCaseBinding {
        return FragmentFtueAuthUseCaseBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        views.useCaseOptionOne.setUseCase(R.string.ftue_auth_use_case_option_one, FtueUseCase.FRIENDS_FAMILY)
        views.useCaseOptionTwo.setUseCase(R.string.ftue_auth_use_case_option_two, FtueUseCase.TEAMS)
        views.useCaseOptionThree.setUseCase(R.string.ftue_auth_use_case_option_three, FtueUseCase.COMMUNITIES)

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
        // Nothing to do
    }

    private fun TextView.setUseCase(@StringRes label: Int, useCase: FtueUseCase) {
        setText(label)
        debouncedClicks {
            viewModel.handle(OnboardingAction.UpdateUseCase(useCase))
        }
    }
}
