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
import im.vector.app.databinding.FragmentDebugMemoryLeaksBinding

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
