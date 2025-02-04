/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
