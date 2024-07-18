/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.crypto.recover

import android.view.LayoutInflater
import android.view.ViewGroup
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentBootstrapErrorBinding
import im.vector.lib.strings.CommonStrings

@AndroidEntryPoint
class BootstrapErrorFragment :
        VectorBaseFragment<FragmentBootstrapErrorBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBootstrapErrorBinding {
        return FragmentBootstrapErrorBinding.inflate(inflater, container, false)
    }

    val sharedViewModel: BootstrapSharedViewModel by parentFragmentViewModel()

    override fun invalidate() = withState(sharedViewModel) { state ->
        when (state.step) {
            is BootstrapStep.Error -> {
                views.bootstrapDescriptionText.setTextOrHide(errorFormatter.toHumanReadable(state.step.error))
            }
            else -> {
                // Should not happen, show a generic error
                views.bootstrapDescriptionText.setTextOrHide(getString(CommonStrings.unknown_error))
            }
        }
        views.bootstrapRetryButton.onClick {
            sharedViewModel.handle(BootstrapActions.Retry)
        }
    }
}
