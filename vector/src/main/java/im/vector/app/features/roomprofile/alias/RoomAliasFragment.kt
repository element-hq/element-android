/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.roomprofile.alias

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.toast
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomprofile.RoomProfileArgs
import kotlinx.android.synthetic.main.fragment_room_setting_generic.*
import kotlinx.android.synthetic.main.merge_overlay_waiting_view.*
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class RoomAliasFragment @Inject constructor(
        val viewModelFactory: RoomAliasViewModel.Factory,
        private val controller: RoomAliasController,
        private val avatarRenderer: AvatarRenderer
) :
        VectorBaseFragment(),
        RoomAliasController.Callback {

    private val viewModel: RoomAliasViewModel by fragmentViewModel()
    private val roomProfileArgs: RoomProfileArgs by args()

    override fun getLayoutResId() = R.layout.fragment_room_setting_generic

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller.callback = this
        setupToolbar(roomSettingsToolbar)
        roomSettingsRecyclerView.configureWith(controller, hasFixedSize = true)
        waiting_view_status_text.setText(R.string.please_wait)
        waiting_view_status_text.isVisible = true

        viewModel.observeViewEvents {
            when (it) {
                is RoomAliasViewEvents.Failure -> showFailure(it.throwable)
                RoomAliasViewEvents.Success    -> showSuccess()
            }.exhaustive
        }
    }

    private fun showSuccess() {
        activity?.toast(R.string.room_settings_save_success)
    }

    override fun onDestroyView() {
        controller.callback = null
        roomSettingsRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { viewState ->
        controller.setData(viewState)
        renderRoomSummary(viewState)
    }

    private fun renderRoomSummary(state: RoomAliasViewState) {
        waiting_view.isVisible = state.isLoading

        state.roomSummary()?.let {
            roomSettingsToolbarTitleView.text = it.displayName
            avatarRenderer.render(it.toMatrixItem(), roomSettingsToolbarAvatarImageView)
        }

        invalidateOptionsMenu()
    }

    override fun removeAlias(altAlias: String) {
        viewModel.handle(RoomAliasAction.RemoveAlias(altAlias))
    }

    override fun setCanonicalAlias(alias: String) {
        viewModel.handle(RoomAliasAction.SetCanonicalAlias(alias))
    }

    override fun unsetCanonicalAlias() {
        viewModel.handle(RoomAliasAction.UnSetCanonicalAlias)
    }

    override fun addLocalAlias(alias: String) {
        viewModel.handle(RoomAliasAction.AddLocalAlias(alias))
    }

    override fun removeLocalAlias(alias: String) {
        viewModel.handle(RoomAliasAction.RemoveLocalAlias(alias))
    }
}
