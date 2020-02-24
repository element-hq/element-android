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
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.api.util.MatrixItem
import im.vector.riotx.R
import im.vector.riotx.core.animations.AppBarStateChangeListener
import im.vector.riotx.core.animations.MatrixItemAppBarStateChangeListener
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.extensions.setTextOrHide
import im.vector.riotx.core.platform.StateView
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.core.utils.startSharePlainTextIntent
import im.vector.riotx.features.crypto.verification.VerificationBottomSheet
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.roommemberprofile.devices.DeviceListBottomSheet
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_matrix_profile.*
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

    override fun getMenuRes() = R.menu.vector_room_member_profile

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(matrixProfileToolbar)
        val headerView = matrixProfileHeaderView.let {
            it.layoutResource = R.layout.view_stub_room_member_profile_header
            it.inflate()
        }
        memberProfileStateView.eventCallback = object : StateView.EventCallback {
            override fun onRetryClicked() {
                viewModel.handle(RoomMemberProfileAction.RetryFetchingInfo)
            }
        }
        memberProfileStateView.contentView = memberProfileInfoContainer
        matrixProfileRecyclerView.configureWith(roomMemberProfileController, hasFixedSize = true)
        roomMemberProfileController.callback = this
        appBarStateChangeListener = MatrixItemAppBarStateChangeListener(headerView,
                listOf(
                        matrixProfileToolbarAvatarImageView,
                        matrixProfileToolbarTitleView,
                        matrixProfileDecorationToolbarAvatarImageView
                )
        )
        matrixProfileAppBarLayout.addOnOffsetChangedListener(appBarStateChangeListener)
        viewModel.observeViewEvents {
            when (it) {
                is RoomMemberProfileViewEvents.Loading                -> showLoading(it.message)
                is RoomMemberProfileViewEvents.Failure                -> showFailure(it.throwable)
                is RoomMemberProfileViewEvents.OnIgnoreActionSuccess  -> Unit
                is RoomMemberProfileViewEvents.StartVerification      -> handleStartVerification(it)
                is RoomMemberProfileViewEvents.ShareRoomMemberProfile -> handleShareRoomMemberProfile(it.permalink)
            }.exhaustive
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.roomMemberProfileShareAction -> {
                viewModel.handle(RoomMemberProfileAction.ShareRoomMemberProfile)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleStartVerification(startVerification: RoomMemberProfileViewEvents.StartVerification) {
        if (startVerification.canCrossSign) {
            VerificationBottomSheet
                    .withArgs(roomId = null, otherUserId = startVerification.userId)
                    .show(parentFragmentManager, "VERIF")
        } else {
            AlertDialog.Builder(requireContext())
                    .setTitle(R.string.dialog_title_warning)
                    .setMessage(R.string.verify_cannot_cross_sign)
                    .setPositiveButton(R.string.verification_profile_verify) { _, _ ->
                        VerificationBottomSheet
                                .withArgs(roomId = null, otherUserId = startVerification.userId)
                                .show(parentFragmentManager, "VERIF")
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }
    }

    override fun onDestroyView() {
        matrixProfileAppBarLayout.removeOnOffsetChangedListener(appBarStateChangeListener)
        roomMemberProfileController.callback = null
        matrixProfileRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        when (val asyncUserMatrixItem = state.userMatrixItem) {
            is Incomplete -> {
                matrixProfileToolbarTitleView.text = state.userId
                avatarRenderer.render(MatrixItem.UserItem(state.userId, null, null), matrixProfileToolbarAvatarImageView)
                memberProfileStateView.state = StateView.State.Loading
            }
            is Fail       -> {
                avatarRenderer.render(MatrixItem.UserItem(state.userId, null, null), matrixProfileToolbarAvatarImageView)
                matrixProfileToolbarTitleView.text = state.userId
                val failureMessage = errorFormatter.toHumanReadable(asyncUserMatrixItem.error)
                memberProfileStateView.state = StateView.State.Error(failureMessage)
            }
            is Success    -> {
                val userMatrixItem = asyncUserMatrixItem()
                memberProfileStateView.state = StateView.State.Content
                memberProfileIdView.text = userMatrixItem.id
                val bestName = userMatrixItem.getBestName()
                memberProfileNameView.text = bestName
                matrixProfileToolbarTitleView.text = bestName
                avatarRenderer.render(userMatrixItem, memberProfileAvatarView)
                avatarRenderer.render(userMatrixItem, matrixProfileToolbarAvatarImageView)

                if (state.isRoomEncrypted) {
                    memberProfileDecorationImageView.isVisible = true
                    if (state.userMXCrossSigningInfo != null) {
                        // Cross signing is enabled for this user
                        val icon = if (state.userMXCrossSigningInfo.isTrusted()) {
                            // User is trusted
                            if (state.allDevicesAreCrossSignedTrusted) {
                                R.drawable.ic_shield_trusted
                            } else {
                                R.drawable.ic_shield_warning
                            }
                        } else {
                            R.drawable.ic_shield_black
                        }

                        memberProfileDecorationImageView.setImageResource(icon)
                        matrixProfileDecorationToolbarAvatarImageView.setImageResource(icon)
                    } else {
                        // Legacy
                        if (state.allDevicesAreTrusted) {
                            memberProfileDecorationImageView.setImageResource(R.drawable.ic_shield_trusted)
                            matrixProfileDecorationToolbarAvatarImageView.setImageResource(R.drawable.ic_shield_trusted)
                        } else {
                            memberProfileDecorationImageView.setImageResource(R.drawable.ic_shield_warning)
                            matrixProfileDecorationToolbarAvatarImageView.setImageResource(R.drawable.ic_shield_warning)
                        }
                    }
                } else {
                    memberProfileDecorationImageView.isVisible = false
                }
            }
        }
        memberProfilePowerLevelView.setTextOrHide(state.userPowerLevelString())
        roomMemberProfileController.setData(state)
    }

    // RoomMemberProfileController.Callback

    override fun onIgnoreClicked() {
        viewModel.handle(RoomMemberProfileAction.IgnoreUser)
    }

    override fun onTapVerify() {
        viewModel.handle(RoomMemberProfileAction.VerifyUser)
    }

    override fun onShowDeviceList() = withState(viewModel) {
        DeviceListBottomSheet.newInstance(it.userId).show(parentFragmentManager, "DEV_LIST")
    }

    override fun onShowDeviceListNoCrossSigning() = withState(viewModel) {
        DeviceListBottomSheet.newInstance(it.userId).show(parentFragmentManager, "DEV_LIST")
    }

    override fun onJumpToReadReceiptClicked() {
        vectorBaseActivity.notImplemented("Jump to read receipts")
    }

    override fun onMentionClicked() {
        vectorBaseActivity.notImplemented("Mention")
    }

    private fun handleShareRoomMemberProfile(permalink: String) {
        startSharePlainTextIntent(fragment = this, chooserTitle = null, text = permalink)
    }
}
