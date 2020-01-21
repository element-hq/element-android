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

package im.vector.riotx.features.roomprofile.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.riotx.R
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.roomprofile.RoomProfileArgs
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_room_setting_generic.*
import kotlinx.android.synthetic.main.merge_overlay_waiting_view.*
import javax.inject.Inject

class RoomSettingsFragment @Inject constructor(
        val viewModelFactory: RoomSettingsViewModel.Factory,
        private val controller: RoomSettingsController,
        private val avatarRenderer: AvatarRenderer
) : VectorBaseFragment(), RoomSettingsController.Callback {

    private val viewModel: RoomSettingsViewModel by fragmentViewModel()
    private val roomProfileArgs: RoomProfileArgs by args()

    override fun getLayoutResId() = R.layout.fragment_room_setting_generic

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller.callback = this
        setupToolbar(roomSettingsToolbar)
        recyclerView.configureWith(controller, hasFixedSize = true)
        waiting_view_status_text.setText(R.string.please_wait)
        waiting_view_status_text.isVisible = true

        viewModel.viewEvents
                .observe()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    dismissLoadingDialog()
                    when (it) {
                        is RoomSettingsViewEvents.Failure -> showFailure(it.throwable)
                    }
                }
                .disposeOnDestroyView()
    }

    override fun onDestroyView() {
        recyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { viewState ->
        controller.setData(viewState)
        renderRoomSummary(viewState)
    }

    override fun onEnableEncryptionClicked() {
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.room_settings_enable_encryption_dialog_title)
                .setMessage(R.string.room_settings_enable_encryption_dialog_content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.room_settings_enable_encryption_dialog_submit) { _, _ ->
                    viewModel.handle(RoomSettingsAction.EnableEncryption)
                }
                .show()
    }

    private fun renderRoomSummary(state: RoomSettingsViewState) {
        waiting_view.isVisible = state.isLoading

        state.roomSummary()?.let {
            roomSettingsToolbarTitleView.text = it.displayName
            avatarRenderer.render(it.toMatrixItem(), roomSettingsToolbarAvatarImageView)
        }
    }
}
