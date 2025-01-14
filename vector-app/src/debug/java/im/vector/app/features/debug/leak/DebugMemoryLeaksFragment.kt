/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.debug.leak

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.application.databinding.FragmentDebugMemoryLeaksBinding

@AndroidEntryPoint
class DebugMemoryLeaksFragment :
        VectorBaseFragment<FragmentDebugMemoryLeaksBinding>() {

    private val viewModel: DebugMemoryLeaksViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentDebugMemoryLeaksBinding {
        return FragmentDebugMemoryLeaksBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViewListeners()
    }

    private fun setViewListeners() {
        views.enableMemoryLeakAnalysis.onClick {
            viewModel.handle(DebugMemoryLeaksViewActions.EnableMemoryLeaksAnalysis(views.enableMemoryLeakAnalysis.isChecked))
        }
    }

    override fun invalidate() = withState(viewModel) { viewState ->
        views.enableMemoryLeakAnalysis.isChecked = viewState.isMemoryLeaksAnalysisEnabled
    }
}
