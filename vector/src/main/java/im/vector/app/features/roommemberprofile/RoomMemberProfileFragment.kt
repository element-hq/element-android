/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roommemberprofile

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.animations.AppBarStateChangeListener
import im.vector.app.core.animations.MatrixItemAppBarStateChangeListener
import im.vector.app.core.dialogs.ConfirmationDialogBuilder
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.copyOnLongClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.StateView
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.utils.startSharePlainTextIntent
import im.vector.app.databinding.DialogBaseEditTextBinding
import im.vector.app.databinding.DialogShareQrCodeBinding
import im.vector.app.databinding.FragmentMatrixProfileBinding
import im.vector.app.databinding.ViewStubRoomMemberProfileHeaderBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.crypto.verification.user.UserVerificationBottomSheet
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.RoomDetailPendingAction
import im.vector.app.features.home.room.detail.RoomDetailPendingActionStore
import im.vector.app.features.home.room.detail.timeline.helper.MatrixItemColorProvider
import im.vector.app.features.roommemberprofile.devices.DeviceListBottomSheet
import im.vector.app.features.roommemberprofile.powerlevel.EditPowerLevelDialogs
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.crypto.model.UserVerificationLevel
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import org.matrix.android.sdk.api.util.MatrixItem
import javax.inject.Inject

@Parcelize
data class RoomMemberProfileArgs(
        val userId: String,
        val roomId: String? = null
) : Parcelable

@AndroidEntryPoint
class RoomMemberProfileFragment :
        VectorBaseFragment<FragmentMatrixProfileBinding>(),
        RoomMemberProfileController.Callback,
        VectorMenuProvider {

    @Inject lateinit var roomMemberProfileController: RoomMemberProfileController
    @Inject lateinit var avatarRenderer: AvatarRenderer
    @Inject lateinit var roomDetailPendingActionStore: RoomDetailPendingActionStore
    @Inject lateinit var matrixItemColorProvider: MatrixItemColorProvider

    private lateinit var headerViews: ViewStubRoomMemberProfileHeaderBinding

    private val fragmentArgs: RoomMemberProfileArgs by args()
    private val viewModel: RoomMemberProfileViewModel by fragmentViewModel()

    private var appBarStateChangeListener: AppBarStateChangeListener? = null

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMatrixProfileBinding {
        return FragmentMatrixProfileBinding.inflate(inflater, container, false)
    }

    override fun getMenuRes() = R.menu.vector_room_member_profile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.User
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(views.matrixProfileToolbar)
                .allowBack()
        val headerView = views.matrixProfileHeaderView.let {
            it.layoutResource = R.layout.view_stub_room_member_profile_header
            it.inflate()
        }
        headerViews = ViewStubRoomMemberProfileHeaderBinding.bind(headerView)
        headerViews.memberProfileStateView.eventCallback = object : StateView.EventCallback {
            override fun onRetryClicked() {
                viewModel.handle(RoomMemberProfileAction.RetryFetchingInfo)
            }
        }
        headerViews.memberProfileStateView.contentView = headerViews.memberProfileInfoContainer
        views.matrixProfileRecyclerView.configureWith(roomMemberProfileController, hasFixedSize = true, disableItemAnimation = true)
        roomMemberProfileController.callback = this
        appBarStateChangeListener = MatrixItemAppBarStateChangeListener(
                headerView,
                listOf(
                        views.matrixProfileToolbarAvatarImageView,
                        views.matrixProfileToolbarTitleView,
                        views.matrixProfileDecorationToolbarAvatarImageView
                )
        )
        views.matrixProfileAppBarLayout.addOnOffsetChangedListener(appBarStateChangeListener)
        viewModel.observeViewEvents {
            when (it) {
                is RoomMemberProfileViewEvents.Loading -> showLoading(it.message)
                is RoomMemberProfileViewEvents.Failure -> showFailure(it.throwable)
                is RoomMemberProfileViewEvents.StartVerification -> handleStartVerification(it)
                is RoomMemberProfileViewEvents.ShareRoomMemberProfile -> handleShareRoomMemberProfile(it.permalink)
                is RoomMemberProfileViewEvents.ShowPowerLevelValidation -> handleShowPowerLevelAdminWarning(it)
                is RoomMemberProfileViewEvents.ShowPowerLevelDemoteWarning -> handleShowPowerLevelDemoteWarning(it)
                is RoomMemberProfileViewEvents.OpenRoom -> handleOpenRoom(it)
                is RoomMemberProfileViewEvents.OnKickActionSuccess -> Unit
                is RoomMemberProfileViewEvents.OnSetPowerLevelSuccess -> Unit
                is RoomMemberProfileViewEvents.OnBanActionSuccess -> Unit
                is RoomMemberProfileViewEvents.OnIgnoreActionSuccess -> Unit
                is RoomMemberProfileViewEvents.OnInviteActionSuccess -> Unit
                RoomMemberProfileViewEvents.GoBack -> handleGoBack()
                RoomMemberProfileViewEvents.OnReportActionSuccess -> handleReportSuccess()
            }
        }
        setupLongClicks()
    }

    private fun handleReportSuccess() {
        MaterialAlertDialogBuilder(requireContext())
                .setTitle(CommonStrings.user_reported_as_inappropriate_title)
                .setMessage(CommonStrings.user_reported_as_inappropriate_content)
                .setPositiveButton(CommonStrings.ok, null)
                .show()
    }

    private fun setupLongClicks() {
        headerViews.memberProfileNameView.copyOnLongClick()
        headerViews.memberProfileIdView.copyOnLongClick()
    }

    private fun handleOpenRoom(event: RoomMemberProfileViewEvents.OpenRoom) {
        navigator.openRoom(requireContext(), event.roomId, null)
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

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.roomMemberProfileShareAction -> {
                viewModel.handle(RoomMemberProfileAction.ShareRoomMemberProfile)
                true
            }
            else -> false
        }
    }

    private fun handleStartVerification(startVerification: RoomMemberProfileViewEvents.StartVerification) {
        if (startVerification.canCrossSign) {
            UserVerificationBottomSheet
                    .verifyUser(otherUserId = startVerification.userId)
                    .show(parentFragmentManager, "VERIF")
        } else {
            MaterialAlertDialogBuilder(requireContext())
                    .setTitle(CommonStrings.dialog_title_warning)
                    .setMessage(CommonStrings.verify_cannot_cross_sign)
                    .setPositiveButton(CommonStrings.verification_profile_verify) { _, _ ->
                        UserVerificationBottomSheet
                                .verifyUser(otherUserId = startVerification.userId)
                                .show(parentFragmentManager, "VERIF")
                    }
                    .setNegativeButton(CommonStrings.action_cancel, null)
                    .show()
        }
    }

    override fun onDestroyView() {
        views.matrixProfileAppBarLayout.removeOnOffsetChangedListener(appBarStateChangeListener)
        roomMemberProfileController.callback = null
        appBarStateChangeListener = null
        views.matrixProfileRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        when (val asyncUserMatrixItem = state.userMatrixItem) {
            Uninitialized,
            is Loading -> {
                views.matrixProfileToolbarTitleView.text = state.userId
                avatarRenderer.render(MatrixItem.UserItem(state.userId, null, null), views.matrixProfileToolbarAvatarImageView)
                headerViews.memberProfileStateView.state = StateView.State.Loading
            }
            is Fail -> {
                avatarRenderer.render(MatrixItem.UserItem(state.userId, null, null), views.matrixProfileToolbarAvatarImageView)
                views.matrixProfileToolbarTitleView.text = state.userId
                val failureMessage = errorFormatter.toHumanReadable(asyncUserMatrixItem.error)
                headerViews.memberProfileStateView.state = StateView.State.Error(failureMessage)
            }
            is Success -> {
                val userMatrixItem = asyncUserMatrixItem()
                headerViews.memberProfileStateView.state = StateView.State.Content
                headerViews.memberProfileIdView.text = userMatrixItem.id
                val bestName = userMatrixItem.getBestName()
                headerViews.memberProfileNameView.text = bestName
                headerViews.memberProfileNameView.setTextColor(matrixItemColorProvider.getColor(userMatrixItem))
                views.matrixProfileToolbarTitleView.text = bestName
                avatarRenderer.render(userMatrixItem, headerViews.memberProfileAvatarView)
                avatarRenderer.render(userMatrixItem, views.matrixProfileToolbarAvatarImageView)

                if (state.isRoomEncrypted) {
                    headerViews.memberProfileDecorationImageView.isVisible = true
                    val trustLevel = if (state.userMXCrossSigningInfo != null) {
                        // Cross signing is enabled for this user
                        if (state.userMXCrossSigningInfo.isTrusted()) {
                            // User is trusted
                            if (state.allDevicesAreCrossSignedTrusted) {
                                UserVerificationLevel.VERIFIED_ALL_DEVICES_TRUSTED
                            } else {
                                UserVerificationLevel.VERIFIED_WITH_DEVICES_UNTRUSTED
                            }
                        } else {
                            if (state.userMXCrossSigningInfo.wasTrustedOnce) {
                                UserVerificationLevel.UNVERIFIED_BUT_WAS_PREVIOUSLY
                            } else {
                                UserVerificationLevel.WAS_NEVER_VERIFIED
                            }
                        }
                    } else {
                        // Legacy
                        if (state.allDevicesAreTrusted) {
                            UserVerificationLevel.VERIFIED_ALL_DEVICES_TRUSTED
                        } else {
                            UserVerificationLevel.VERIFIED_WITH_DEVICES_UNTRUSTED
                        }
                    }
                    headerViews.memberProfileDecorationImageView.renderUser(trustLevel)
                    views.matrixProfileDecorationToolbarAvatarImageView.renderUser(trustLevel)
                } else {
                    headerViews.memberProfileDecorationImageView.isVisible = false
                }

                headerViews.memberProfileAvatarView.setOnClickListener { view ->
                    onAvatarClicked(view, userMatrixItem)
                }
                views.matrixProfileToolbarAvatarImageView.setOnClickListener { view ->
                    onAvatarClicked(view, userMatrixItem)
                }
            }
        }
        headerViews.memberProfilePowerLevelView.setTextOrHide(state.userPowerLevelString())
        roomMemberProfileController.setData(state)
    }

    // RoomMemberProfileController.Callback

    override fun onIgnoreClicked() = withState(viewModel) { state ->
        val isIgnored = state.isIgnored() ?: false
        val titleRes: Int
        val positiveButtonRes: Int
        val confirmationRes: Int
        if (isIgnored) {
            confirmationRes = CommonStrings.room_participants_action_unignore_prompt_msg
            titleRes = CommonStrings.room_participants_action_unignore_title
            positiveButtonRes = CommonStrings.room_participants_action_unignore
        } else {
            confirmationRes = CommonStrings.room_participants_action_ignore_prompt_msg
            titleRes = CommonStrings.room_participants_action_ignore_title
            positiveButtonRes = CommonStrings.room_participants_action_ignore
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

    override fun onReportClicked() {
        viewModel.handle(RoomMemberProfileAction.ReportUser)
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

    override fun onOpenDmClicked() {
        viewModel.handle(RoomMemberProfileAction.OpenOrCreateDm(fragmentArgs.userId))
    }

    private fun handleGoBack() {
        roomDetailPendingActionStore.data = RoomDetailPendingAction.DoNothing
        vectorBaseActivity.finish()
    }

    override fun onJumpToReadReceiptClicked() {
        roomDetailPendingActionStore.data = RoomDetailPendingAction.JumpToReadReceipt(fragmentArgs.userId)
        vectorBaseActivity.finish()
    }

    override fun onMentionClicked() {
        roomDetailPendingActionStore.data = RoomDetailPendingAction.MentionUser(fragmentArgs.userId)
        vectorBaseActivity.finish()
    }

    private fun handleShareRoomMemberProfile(permalink: String) {
        val view = layoutInflater.inflate(R.layout.dialog_share_qr_code, null)
        val views = DialogShareQrCodeBinding.bind(view)
        views.itemShareQrCodeImage.setData(permalink)
        MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setNeutralButton(CommonStrings.ok, null)
                .setPositiveButton(CommonStrings.share_by_text) { _, _ ->
                    startSharePlainTextIntent(
                            context = requireContext(),
                            activityResultLauncher = null,
                            chooserTitle = null,
                            text = permalink
                    )
                }.show()
    }

    private fun onAvatarClicked(view: View, userMatrixItem: MatrixItem) {
        navigator.openBigImageViewer(requireActivity(), view, userMatrixItem)
    }

    override fun onOverrideColorClicked(): Unit = withState(viewModel) { state ->
        val inflater = requireActivity().layoutInflater
        val layout = inflater.inflate(R.layout.dialog_base_edit_text, null)
        val views = DialogBaseEditTextBinding.bind(layout)
        views.editText.setText(state.userColorOverride)
        views.editText.hint = "#000000"

        MaterialAlertDialogBuilder(requireContext())
                .setTitle(CommonStrings.room_member_override_nick_color)
                .setView(layout)
                .setPositiveButton(CommonStrings.ok) { _, _ ->
                    val newColor = views.editText.text.toString()
                    if (newColor != state.userColorOverride) {
                        viewModel.handle(RoomMemberProfileAction.SetUserColorOverride(newColor))
                    }
                }
                .setNegativeButton(CommonStrings.action_cancel, null)
                .show()
    }

    override fun onEditPowerLevel(currentRole: Role) {
        EditPowerLevelDialogs.showChoice(requireActivity(), CommonStrings.power_level_edit_title, currentRole) { newPowerLevel ->
            viewModel.handle(RoomMemberProfileAction.SetPowerLevel(currentRole.value, newPowerLevel, true))
        }
    }

    override fun onKickClicked(isSpace: Boolean) {
        ConfirmationDialogBuilder
                .show(
                        activity = requireActivity(),
                        askForReason = true,
                        confirmationRes = if (isSpace) CommonStrings.space_participants_remove_prompt_msg
                        else CommonStrings.room_participants_remove_prompt_msg,
                        positiveRes = CommonStrings.room_participants_action_remove,
                        reasonHintRes = CommonStrings.room_participants_remove_reason,
                        titleRes = CommonStrings.room_participants_remove_title
                ) { reason ->
                    viewModel.handle(RoomMemberProfileAction.KickUser(reason))
                }
    }

    override fun onBanClicked(isSpace: Boolean, isUserBanned: Boolean) {
        val titleRes: Int
        val positiveButtonRes: Int
        val confirmationRes: Int
        if (isUserBanned) {
            confirmationRes = if (isSpace) CommonStrings.space_participants_unban_prompt_msg
            else CommonStrings.room_participants_unban_prompt_msg
            titleRes = CommonStrings.room_participants_unban_title
            positiveButtonRes = CommonStrings.room_participants_action_unban
        } else {
            confirmationRes = if (isSpace) CommonStrings.space_participants_ban_prompt_msg
            else CommonStrings.room_participants_ban_prompt_msg
            titleRes = CommonStrings.room_participants_ban_title
            positiveButtonRes = CommonStrings.room_participants_action_ban
        }
        ConfirmationDialogBuilder
                .show(
                        activity = requireActivity(),
                        askForReason = !isUserBanned,
                        confirmationRes = confirmationRes,
                        positiveRes = positiveButtonRes,
                        reasonHintRes = CommonStrings.room_participants_ban_reason,
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
                        confirmationRes = CommonStrings.room_participants_action_cancel_invite_prompt_msg,
                        positiveRes = CommonStrings.room_participants_action_cancel_invite,
                        reasonHintRes = 0,
                        titleRes = CommonStrings.room_participants_action_cancel_invite_title
                ) {
                    viewModel.handle(RoomMemberProfileAction.KickUser(null))
                }
    }

    override fun onInviteClicked() {
        viewModel.handle(RoomMemberProfileAction.InviteUser)
    }
}
