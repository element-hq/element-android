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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentBootstrapWaitingBinding
import javax.inject.Inject

class BootstrapWaitingFragment @Inject constructor() :
    VectorBaseFragment<FragmentBootstrapWaitingBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBootstrapWaitingBinding {
        return FragmentBootstrapWaitingBinding.inflate(inflater, container, false)
    }

    val sharedViewModel: BootstrapSharedViewModel by parentFragmentViewModel()

    override fun invalidate() = withState(sharedViewModel) { state ->
        when (state.step) {
            is BootstrapStep.Initializing -> {
                views.bootstrapLoadingStatusText.isVisible = true
                views.bootstrapDescriptionText.isVisible = true
                views.bootstrapLoadingStatusText.text = state.initializationWaitingViewData?.message
            }
//            is BootstrapStep.CheckingMigration -> {
//                bootstrapLoadingStatusText.isVisible = false
//                bootstrapDescriptionText.isVisible = false
//            }
            else                          -> {
                // just show the spinner
                views.bootstrapLoadingStatusText.isVisible = false
                views.bootstrapDescriptionText.isVisible = false
            }
        }
    }
}
