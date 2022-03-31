/*
 * Copyright (c) 2022 New Vector Ltd
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
import im.vector.app.core.extensions.content
import im.vector.app.core.extensions.editText
import im.vector.app.core.extensions.realignPercentagesToParent
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.databinding.FragmentFtueServerSelectionCombinedBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.onboarding.OnboardingViewState
import javax.inject.Inject

class FtueAuthCombinedServerSelectionFragment @Inject constructor() : AbstractFtueAuthFragment<FragmentFtueServerSelectionCombinedBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueServerSelectionCombinedBinding {
        return FragmentFtueServerSelectionCombinedBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.chooseServerRoot.realignPercentagesToParent()

        views.chooseServerToolbar.setNavigationOnClickListener {
            viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnBack))
        }

        views.chooseServerSubmit.debouncedClicks {
            viewModel.handle(OnboardingAction.UpdateHomeServer(views.chooseServerInput.content()))
        }
    }

    override fun resetViewModel() {
        // do nothing
    }

    override fun updateWithState(state: OnboardingViewState) {
        val userUrlInput = state.serverSelectionState.userUrlInput?.toReducedUrlKeepingSchemaIfInsecure()
        views.chooseServerInput.editText().setText(userUrlInput)
    }

    private fun String.toReducedUrlKeepingSchemaIfInsecure() = toReducedUrl(keepSchema = this.startsWith("http://"))
}
