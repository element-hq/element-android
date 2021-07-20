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

package im.vector.app.features.roomprofile.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.fragmentViewModel
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.ViewEditRoomNotificationSettingsBinding
import im.vector.app.features.home.AvatarRenderer
import javax.inject.Inject

/**
 * In this screen:
 * - the account has been created and we propose the user to set an avatar and a display name
 */
class RoomNotificationSettingsFragment @Inject constructor(
        val roomNotificationSettingsViewModel: RoomNotificationSettingsViewModel.Factory,
        private val avatarRenderer: AvatarRenderer,
) : VectorBaseFragment<ViewEditRoomNotificationSettingsBinding>() {

    private val viewModel: RoomNotificationSettingsViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): ViewEditRoomNotificationSettingsBinding {
        return ViewEditRoomNotificationSettingsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewEvents()
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents {
            when (it) {
                is RoomNotificationSettingsViewEvents.Failure -> displayErrorDialog(it.throwable)
                RoomNotificationSettingsViewEvents.SaveComplete -> TODO()
            }
        }
    }

}
