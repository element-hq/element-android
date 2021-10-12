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

package im.vector.app.features.roomprofile.settings.linkaccess

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentRoomSettingGenericBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomprofile.settings.linkaccess.detail.TchapRoomLinkAccessBottomSheet
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class TchapRoomLinkAccessFragment @Inject constructor(
        val viewModelFactory: TchapRoomLinkAccessViewModel.Factory,
        val controller: TchapRoomLinkAccessController,
        private val avatarRenderer: AvatarRenderer
) : VectorBaseFragment<FragmentRoomSettingGenericBinding>(),
        TchapRoomLinkAccessController.InteractionListener {

    private val viewModel: TchapRoomLinkAccessViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            FragmentRoomSettingGenericBinding.inflate(inflater, container, false)

    override fun invalidate() = withState(viewModel) { state ->
        controller.setData(state)
        renderRoomSummary(state)
    }

    private fun renderRoomSummary(state: TchapRoomLinkAccessState) {
        state.roomSummary()?.let {
            views.roomSettingsToolbarTitleView.text = it.displayName
            avatarRenderer.render(it.toMatrixItem(), views.roomSettingsToolbarAvatarImageView)
            views.roomSettingsDecorationToolbarAvatarImageView.render(it.roomEncryptionTrustLevel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller.interactionListener = this
        setupToolbar(views.roomSettingsToolbar)
        views.roomSettingsRecyclerView.configureWith(controller, hasFixedSize = true)
    }

    override fun onDestroyView() {
        controller.interactionListener = null
        views.roomSettingsRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun setLinkAccessEnabled(isEnabled: Boolean) {
        viewModel.handle(TchapRoomLinkAccessAction.SetIsEnabled(isEnabled))
    }

    override fun openAliasDetail(alias: String) {
        TchapRoomLinkAccessBottomSheet
                .newInstance(alias = alias)
                .show(childFragmentManager, "TCHAP_ROOM_LINK_ACCESS_ACTIONS")
    }
}
