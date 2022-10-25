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

package im.vector.app.features.settings.font

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.restart
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSettingsFontScalingBinding
import im.vector.app.features.settings.FontScaleValue
import javax.inject.Inject

@AndroidEntryPoint
class FontScaleSettingFragment :
        VectorBaseFragment<FragmentSettingsFontScalingBinding>(),
        FontScaleSettingController.Callback {

    @Inject lateinit var fontListController: FontScaleSettingController

    private val viewModel: FontScaleSettingViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsFontScalingBinding {
        return FragmentSettingsFontScalingBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(views.fontScaleToolbar)
                .allowBack()

        fontListController.callback = this
        setupRecyclerView()

        viewModel.observeViewEvents {
            when (it) {
                is FontScaleSettingViewEvents.RestartActivity -> {
                    requireActivity().restart()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        views.fonsScaleRecycler.configureWith(fontListController)
    }

    override fun invalidate() = withState(viewModel) { state ->
        fontListController.setData(state)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fontListController.callback = null
    }

    override fun onUseSystemSettingChanged(useSystemSettings: Boolean) {
        viewModel.handle(FontScaleSettingAction.UseSystemSettingChangedAction(useSystemSettings))
    }

    override fun oFontScaleSelected(fonScale: FontScaleValue) {
        viewModel.handle(FontScaleSettingAction.FontScaleChangedAction(fonScale))
    }
}
