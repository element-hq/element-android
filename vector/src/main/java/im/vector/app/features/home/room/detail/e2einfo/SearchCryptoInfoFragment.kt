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

package im.vector.app.features.home.room.detail.e2einfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentRecyclerviewWithSearchBinding
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.internal.crypto.IncomingRoomKeyRequest
import reactivecircus.flowbinding.appcompat.queryTextChanges
import javax.inject.Inject

class SearchCryptoInfoFragment @Inject constructor(
        private val controller: SearchCryptoInfoController
) : VectorBaseFragment<FragmentRecyclerviewWithSearchBinding>(), CryptoInfoController.Callback {

    val viewModel: CryptoInfoViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentRecyclerviewWithSearchBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.addRoomToSpaceToolbar.isGone = true
        setupSearchView()
        views.roomList.configureWith(controller, disableItemAnimation = true)
        controller.callback = this
    }

    override fun onResume() {
        super.onResume()
        withState(viewModel) {
            if (it.searchFilter.isNullOrBlank()) {
                views.memberNameFilter.requestFocus()
            }
        }
    }

    private fun setupSearchView() {
        views.memberNameFilter.queryHint = getString(R.string.search_device_user_hint)
        views.memberNameFilter.queryTextChanges()
                .debounce(100)
                .onEach {
                    viewModel.handle(CryptoInfoAction.FilterContent(it.toString()))
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        controller.callback = null
        views.roomList.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        controller.setData(state)
    }

    override fun forceShare(request: IncomingRoomKeyRequest) {
        viewModel.handle(CryptoInfoAction.ReviewRequest(request))
    }

    override fun reRequestKeysForEvent() {
        viewModel.handle(CryptoInfoAction.ReRequestKey)
    }
}
