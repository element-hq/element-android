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
 *
 */

package im.vector.riotx.features.roommemberprofile

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.api.session.room.powerlevers.PowerLevelsConstants
import im.vector.matrix.android.api.session.room.powerlevers.PowerLevelsHelper
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.riotx.R
import im.vector.riotx.core.animations.AppBarStateChangeListener
import im.vector.riotx.core.animations.MatrixItemAppBarStateChangeListener
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.extensions.setTextOrHide
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.home.AvatarRenderer
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_matrix_profile.*
import kotlinx.android.synthetic.main.fragment_matrix_profile.matrixProfileHeaderView
import kotlinx.android.synthetic.main.view_stub_room_member_profile_header.*
import javax.inject.Inject

@Parcelize
data class RoomMemberProfileArgs(
        val userId: String,
        val roomId: String? = null
) : Parcelable

class RoomMemberProfileFragment @Inject constructor(
        val viewModelFactory: RoomMemberProfileViewModel.Factory,
        private val roomMemberProfileController: RoomMemberProfileController,
        private val avatarRenderer: AvatarRenderer
) : VectorBaseFragment(), RoomMemberProfileController.Callback {

    private val fragmentArgs: RoomMemberProfileArgs by args()
    private val viewModel: RoomMemberProfileViewModel by fragmentViewModel()

    private lateinit var appBarStateChangeListener: AppBarStateChangeListener

    override fun getLayoutResId() = R.layout.fragment_matrix_profile

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(matrixProfileToolbar)
        matrixProfileHeaderView.apply {
            layoutResource = R.layout.view_stub_room_member_profile_header
            inflate()
        }
        matrixProfileRecyclerView.configureWith(roomMemberProfileController, hasFixedSize = true)
        roomMemberProfileController.callback = this
        appBarStateChangeListener = MatrixItemAppBarStateChangeListener(matrixProfileCollapsingToolbarLayout.scrimAnimationDuration, listOf(matrixProfileToolbarAvatarImageView, matrixProfileToolbarTitleView))
        matrixProfileAppBarLayout.addOnOffsetChangedListener(appBarStateChangeListener)
    }

    override fun onDestroyView() {
        matrixProfileAppBarLayout.removeOnOffsetChangedListener(appBarStateChangeListener)
        roomMemberProfileController.callback = null
        matrixProfileRecyclerView.cleanup()
        super.onDestroyView()
    }


    override fun invalidate() = withState(viewModel) { state ->
        val memberMatrixItem = state.memberAsMatrixItem() ?: return@withState
        memberProfileIdView.text = memberMatrixItem.id
        val bestName = memberMatrixItem.getBestName()
        memberProfileNameView.text = bestName
        matrixProfileToolbarTitleView.text = bestName
        avatarRenderer.render(memberMatrixItem, memberProfileAvatarView)
        avatarRenderer.render(memberMatrixItem, matrixProfileToolbarAvatarImageView)

        val roomSummary = state.roomSummary()
        val powerLevelsContent = state.powerLevelsContent()
        if (powerLevelsContent == null || roomSummary == null) {
            memberProfilePowerLevelView.visibility = View.GONE
        } else {
            val roomName = roomSummary.toMatrixItem().getBestName()
            val powerLevelsHelper = PowerLevelsHelper(powerLevelsContent)
            val userPowerLevel = powerLevelsHelper.getUserPowerLevel(state.userId)
            val powerLevelText = if (userPowerLevel == PowerLevelsConstants.DEFAULT_ROOM_ADMIN_LEVEL) {
                getString(R.string.room_member_power_level_admin_in, roomName)
            } else if (userPowerLevel == PowerLevelsConstants.DEFAULT_ROOM_MODERATOR_LEVEL) {
                getString(R.string.room_member_power_level_moderator_in, roomName)
            } else if (userPowerLevel == PowerLevelsConstants.DEFAULT_ROOM_USER_LEVEL) {
                null
            } else {
                getString(R.string.room_member_power_level_custom_in, userPowerLevel, roomName)
            }
            memberProfilePowerLevelView.setTextOrHide(powerLevelText)
        }
        roomMemberProfileController.setData(state)
    }

    // RoomMemberProfileController.Callback

    override fun onIgnoreClicked() {
        vectorBaseActivity.notImplemented("Ignore")
    }

    override fun onLearnMoreClicked() {
        vectorBaseActivity.notImplemented("Learn more")
    }

    override fun onJumpToReadReceiptClicked() {
        vectorBaseActivity.notImplemented("Jump to read receipts")
    }

    override fun onMentionClicked() {
        vectorBaseActivity.notImplemented("Mention")
    }


}
