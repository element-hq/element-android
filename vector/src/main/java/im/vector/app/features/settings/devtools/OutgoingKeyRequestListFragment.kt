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

package im.vector.app.features.settings.devtools

import android.os.Bundle
import android.view.View
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import kotlinx.android.synthetic.main.fragment_generic_recycler.*
import javax.inject.Inject

class OutgoingKeyRequestListFragment @Inject constructor(
        val viewModelFactory: KeyRequestListViewModel.Factory,
        private val epoxyController: KeyRequestEpoxyController,
        private val colorProvider: ColorProvider
) : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_generic_recycler
    private val viewModel: KeyRequestListViewModel by fragmentViewModel(KeyRequestListViewModel::class)

    override fun invalidate() = withState(viewModel) { state ->
        epoxyController.setData(state)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.configureWith(epoxyController, showDivider = true)
//        epoxyController.interactionListener = this
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView.cleanup()
//        epoxyController.interactionListener = null
    }
}
