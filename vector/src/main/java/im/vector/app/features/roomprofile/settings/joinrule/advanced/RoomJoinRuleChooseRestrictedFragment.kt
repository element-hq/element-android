/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.roomprofile.settings.joinrule.advanced

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSpaceRestrictedSelectBinding
import im.vector.app.features.home.AvatarRenderer
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.util.MatrixItem
import reactivecircus.flowbinding.appcompat.queryTextChanges
import javax.inject.Inject

@AndroidEntryPoint
class RoomJoinRuleChooseRestrictedFragment :
        VectorBaseFragment<FragmentSpaceRestrictedSelectBinding>(),
        ChooseRestrictedController.Listener,
        OnBackPressed {

    @Inject lateinit var controller: ChooseRestrictedController
    @Inject lateinit var avatarRenderer: AvatarRenderer

    private val viewModel: RoomJoinRuleChooseRestrictedViewModel by activityViewModel(RoomJoinRuleChooseRestrictedViewModel::class)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            FragmentSpaceRestrictedSelectBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller.listener = this
        views.recyclerView.configureWith(controller)
        views.roomsFilter.queryTextChanges()
                .debounce(500)
                .onEach {
                    viewModel.handle(RoomJoinRuleChooseRestrictedActions.FilterWith(it.toString()))
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        views.okButton.debouncedClicks {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        controller.listener = null
        views.recyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        super.invalidate()
        controller.setData(state)
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        val filter = views.roomsFilter.query
        if (filter.isEmpty()) {
            parentFragmentManager.popBackStack()
        } else {
            views.roomsFilter.setQuery("", true)
        }
        return true
    }

    override fun onItemSelected(matrixItem: MatrixItem) {
        viewModel.handle(RoomJoinRuleChooseRestrictedActions.ToggleSelection(matrixItem))
    }
}
