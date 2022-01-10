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
import im.vector.app.R
import im.vector.app.core.extensions.setTextWithColoredPart
import im.vector.app.databinding.FragmentFtueAuthUseCaseBinding
import im.vector.app.features.login.ServerType
import im.vector.app.features.onboarding.OnboardingAction
import me.saket.bettermovementmethod.BetterLinkMovementMethod
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
        views.useCaseOptionOne.debouncedClicks {
            viewModel.handle(OnboardingAction.UpdateUseCase("todo"))
        }
        views.useCaseOptionTwo.debouncedClicks {
            viewModel.handle(OnboardingAction.UpdateUseCase("todo"))
        }
        views.useCaseOptionThree.debouncedClicks {
            viewModel.handle(OnboardingAction.UpdateUseCase("todo"))
        }

        val partial = getString(R.string.ftue_auth_use_case_skip_partial)
        views.useCaseSkip.setTextWithColoredPart(
                getString(R.string.ftue_auth_use_case_skip, partial),
                partial,
                underline = false,
                colorAttribute = R.attr.colorAccent,
                onClick = { viewModel.handle(OnboardingAction.UpdateUseCase("todo")) }
        )

        views.useCaseConnectToServer.setOnClickListener {
            viewModel.handle(OnboardingAction.UpdateServerType(ServerType.Other))
        }
    }

    override fun resetViewModel() {
        // Nothing to do
    }
}
