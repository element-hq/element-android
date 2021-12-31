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

package im.vector.app.features.crypto.recover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.toSpannable
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.colorizeMatchingText
import im.vector.app.databinding.FragmentBootstrapConclusionBinding
import javax.inject.Inject

class BootstrapConclusionFragment @Inject constructor(
        private val colorProvider: ColorProvider
) : VectorBaseFragment<FragmentBootstrapConclusionBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBootstrapConclusionBinding {
        return FragmentBootstrapConclusionBinding.inflate(inflater, container, false)
    }

    val sharedViewModel: BootstrapSharedViewModel by parentFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.bootstrapConclusionContinue.views.bottomSheetActionClickableZone.debouncedClicks { sharedViewModel.handle(BootstrapActions.Completed) }
    }

    override fun invalidate() = withState(sharedViewModel) { state ->
        if (state.step !is BootstrapStep.DoneSuccess) return@withState

        views.bootstrapConclusionText.text = getString(
                R.string.bootstrap_cross_signing_success,
                getString(R.string.recovery_passphrase),
                getString(R.string.message_key)
        )
                .toSpannable()
                .colorizeMatchingText(getString(R.string.recovery_passphrase), colorProvider.getColorFromAttribute(android.R.attr.textColorLink))
                .colorizeMatchingText(getString(R.string.message_key), colorProvider.getColorFromAttribute(android.R.attr.textColorLink))
    }
}
