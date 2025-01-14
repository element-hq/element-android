/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.recover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentBootstrapWaitingBinding

@AndroidEntryPoint
class BootstrapWaitingFragment :
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
            else -> {
                // just show the spinner
                views.bootstrapLoadingStatusText.isVisible = false
                views.bootstrapDescriptionText.isVisible = false
            }
        }
        views.bootstrapDescriptionText.giveAccessibilityFocusOnce()
    }
}
