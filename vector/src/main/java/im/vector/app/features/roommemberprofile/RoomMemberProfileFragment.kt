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

package im.vector.app.features.roommemberprofile

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
import im.vector.app.R
import im.vector.app.core.animations.AppBarStateChangeListener
import im.vector.app.core.animations.MatrixItemAppBarStateChangeListener
import im.vector.app.core.dialogs.ConfirmationDialogBuilder
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.StateView
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.startSharePlainTextIntent
import im.vector.app.features.crypto.verification.VerificationBottomSheet
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roommemberprofile.devices.DeviceListBottomSheet
import im.vector.app.features.roommemberprofile.powerlevel.EditPowerLevelDialogs
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import org.matrix.android.sdk.api.util.MatrixItem
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

    private var appBarStateChangeListener: AppBarStateChangeListener? = null

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
        matrixProfileRecyclerView.configureWith(roomMemberProfileController, hasFixedSize = true, disableItemAnimation = true)
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
                is RoomMemberProfileViewEvents.Loading                     -> showLoading(it.message)
                is RoomMemberProfileViewEvents.Failure                     -> showFailure(it.throwable)
                is RoomMemberProfileViewEvents.StartVerification           -> handleStartVerification(it)
                is RoomMemberProfileViewEvents.ShareRoomMemberProfile      -> handleShareRoomMemberProfile(it.permalink)
                is RoomMemberProfileViewEvents.ShowPowerLevelValidation    -> handleShowPowerLevelAdminWarning(it)
                is RoomMemberProfileViewEvents.ShowPowerLevelDemoteWarning -> handleShowPowerLevelDemoteWarning(it)
                is RoomMemberProfileViewEvents.OnKickActionSuccess         -> Unit
                is RoomMemberProfileViewEvents.OnSetPowerLevelSuccess      -> Unit
                is RoomMemberProfileViewEvents.OnBanActionSuccess          -> Unit
                is RoomMemberProfileViewEvents.OnIgnoreActionSuccess       -> Unit
                is RoomMemberProfileViewEvents.OnInviteActionSuccess       -> Unit
            }.exhaustive
        }
    }

    private fun handleShowPowerLevelDemoteWarning(event: RoomMemberProfileViewEvents.ShowPowerLevelDemoteWarning) {
        EditPowerLevelDialogs.showDemoteWarning(requireActivity()) {
            viewModel.handle(RoomMemberProfileAction.SetPowerLevel(event.currentValue, event.newValue, false))
        }
    }

    private fun handleShowPowerLevelAdminWarning(event: RoomMemberProfileViewEvents.ShowPowerLevelValidation) {
        EditPowerLevelDialogs.showValidation(requireActivity()) {
            viewModel.handle(RoomMemberProfileAction.SetPowerLevel(event.currentValue, event.newValue, false))
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
        appBarStateChangeListener = null
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

                memberProfileAvatarView.setOnClickListener { view ->
                    onAvatarClicked(view, userMatrixItem)
                }
                matrixProfileToolbarAvatarImageView.setOnClickListener { view ->
                    onAvatarClicked(view, userMatrixItem)
                }
            }
        }
        memberProfilePowerLevelView.setTextOrHide(state.userPowerLevelString())
        roomMemberProfileController.setData(state)
    }

    // RoomMemberProfileController.Callback

    override fun onIgnoreClicked() = withState(viewModel) { state ->
        val isIgnored = state.isIgnored() ?: false
        val titleRes: Int
        val positiveButtonRes: Int
        val confirmationRes: Int
        if (isIgnored) {
            confirmationRes = R.string.room_participants_action_unignore_prompt_msg
            titleRes = R.string.room_participants_action_unignore_title
            positiveButtonRes = R.string.room_participants_action_unignore
        } else {
            confirmationRes = R.string.room_participants_action_ignore_prompt_msg
            titleRes = R.string.room_participants_action_ignore_title
            positiveButtonRes = R.string.room_participants_action_ignore
        }
        ConfirmationDialogBuilder
                .show(
                        activity = requireActivity(),
                        askForReason = false,
                        confirmationRes = confirmationRes,
                        positiveRes = positiveButtonRes,
                        reasonHintRes = 0,
                        titleRes = titleRes
                ) {
                    viewModel.handle(RoomMemberProfileAction.IgnoreUser)
                }
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

    private fun onAvatarClicked(view: View, userMatrixItem: MatrixItem) {
        navigator.openBigImageViewer(requireActivity(), view, userMatrixItem)
    }

    override fun onEditPowerLevel(currentRole: Role) {
        EditPowerLevelDialogs.showChoice(requireActivity(), currentRole) { newPowerLevel ->
            viewModel.handle(RoomMemberProfileAction.SetPowerLevel(currentRole.value, newPowerLevel, true))
        }
    }

    override fun onKickClicked() {
        ConfirmationDialogBuilder
                .show(
                        activity = requireActivity(),
                        askForReason = true,
                        confirmationRes = R.string.room_participants_kick_prompt_msg,
                        positiveRes = R.string.room_participants_action_kick,
                        reasonHintRes = R.string.room_participants_kick_reason,
                        titleRes = R.string.room_participants_kick_title
                ) { reason ->
                    viewModel.handle(RoomMemberProfileAction.KickUser(reason))
                }
    }

    override fun onBanClicked(isUserBanned: Boolean) {
        val titleRes: Int
        val positiveButtonRes: Int
        val confirmationRes: Int
        if (isUserBanned) {
            confirmationRes = R.string.room_participants_unban_prompt_msg
            titleRes = R.string.room_participants_unban_title
            positiveButtonRes = R.string.room_participants_action_unban
        } else {
            confirmationRes = R.string.room_participants_ban_prompt_msg
            titleRes = R.string.room_participants_ban_title
            positiveButtonRes = R.string.room_participants_action_ban
        }
        ConfirmationDialogBuilder
                .show(
                        activity = requireActivity(),
                        askForReason = !isUserBanned,
                        confirmationRes = confirmationRes,
                        positiveRes = positiveButtonRes,
                        reasonHintRes = R.string.room_participants_ban_reason,
                        titleRes = titleRes
                ) { reason ->
                    viewModel.handle(RoomMemberProfileAction.BanOrUnbanUser(reason))
                }
    }

    override fun onCancelInviteClicked() {
        ConfirmationDialogBuilder
                .show(
                        activity = requireActivity(),
                        askForReason = false,
                        confirmationRes = R.string.room_participants_action_cancel_invite_prompt_msg,
                        positiveRes = R.string.room_participants_action_cancel_invite,
                        reasonHintRes = 0,
                        titleRes = R.string.room_participants_action_cancel_invite_title
                ) {
                    viewModel.handle(RoomMemberProfileAction.KickUser(null))
                }
    }

    override fun onInviteClicked() {
        viewModel.handle(RoomMemberProfileAction.InviteUser)
    }
}
