/*
 * Copyright 2019 New Vector Ltd
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
 *
 */

package im.vector.riotx.features.roomprofile

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.extensions.setTextOrHide
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.home.AvatarRenderer
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_room_profile.*
import timber.log.Timber
import javax.inject.Inject

@Parcelize
data class RoomProfileArgs(
        val roomId: String
) : Parcelable

class RoomProfileFragment @Inject constructor(
        private val roomProfileController: RoomProfileController,
        private val avatarRenderer: AvatarRenderer,
        val roomProfileViewModelFactory: RoomProfileViewModel.Factory
) : VectorBaseFragment(), RoomProfileController.Callback {

    private val roomProfileArgs: RoomProfileArgs by args()
    private val roomProfileViewModel: RoomProfileViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_room_profile

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(roomProfileToolbar)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        roomProfileController.callback = this
        roomProfileRecyclerView.setHasFixedSize(true)
        roomProfileRecyclerView.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        roomProfileRecyclerView.adapter = roomProfileController.adapter

    }

    override fun onDestroyView() {
        super.onDestroyView()
        roomProfileRecyclerView.adapter = null
    }

    override fun invalidate() = withState(roomProfileViewModel) { state ->
        state.roomSummary()?.let {
            if (it.membership.isLeft()) {
                Timber.w("The room has been left")
                activity?.finish()
            } else {
                roomProfileNameView.text = it.displayName
                roomProfileNameView2.text = it.displayName
                roomProfileIdView.text = it.roomId
                roomProfileTopicView.setTextOrHide(it.topic)
                avatarRenderer.render(it, roomProfileAvatarView)
            }
        }
        roomProfileController.setData(state)
    }

    // RoomProfileController.Callback

    override fun onLearnMoreClicked() {
        vectorBaseActivity.notImplemented()
    }

    override fun onMemberListClicked() {
        vectorBaseActivity.notImplemented("Room member list")
    }

    override fun onSettingsClicked() {
        vectorBaseActivity.notImplemented("Room settings")
    }


}
