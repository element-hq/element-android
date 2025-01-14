/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail

import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.core.utils.PERMISSIONS_FOR_AUDIO_IP_CALL
import im.vector.app.core.utils.PERMISSIONS_FOR_VIDEO_IP_CALL
import im.vector.app.core.utils.checkPermissions
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.strings.CommonStrings

class StartCallActionsHandler(
        private val roomId: String,
        private val fragment: Fragment,
        private val callManager: WebRtcCallManager,
        private val vectorPreferences: VectorPreferences,
        private val timelineViewModel: TimelineViewModel,
        private val startCallActivityResultLauncher: ActivityResultLauncher<Array<String>>,
        private val showDialogWithMessage: (String) -> Unit,
        private val onTapToReturnToCall: () -> Unit
) {

    fun onVideoCallClicked() {
        handleCallRequest(true)
    }

    fun onVoiceCallClicked() {
        handleCallRequest(false)
    }

    private fun handleCallRequest(isVideoCall: Boolean) = withState(timelineViewModel) { state ->
        if (state.hasActiveElementCallWidget() && !isVideoCall) {
            timelineViewModel.handle(RoomDetailAction.OpenElementCallWidget)
            return@withState
        }

        val roomSummary = state.asyncRoomSummary.invoke() ?: return@withState
        when (roomSummary.joinedMembersCount) {
            1 -> {
                val pendingInvite = roomSummary.invitedMembersCount ?: 0 > 0
                if (pendingInvite) {
                    // wait for other to join
                    showDialogWithMessage(fragment.getString(CommonStrings.cannot_call_yourself_with_invite))
                } else {
                    // You cannot place a call with yourself.
                    showDialogWithMessage(fragment.getString(CommonStrings.cannot_call_yourself))
                }
            }
            2 -> {
                val currentCall = callManager.getCurrentCall()
                if (currentCall?.signalingRoomId == roomId) {
                    onTapToReturnToCall()
                } else if (!state.isAllowedToStartWebRTCCall) {
                    showDialogWithMessage(
                            fragment.getString(
                                    if (state.isDm()) {
                                        CommonStrings.no_permissions_to_start_webrtc_call_in_direct_room
                                    } else {
                                        CommonStrings.no_permissions_to_start_webrtc_call
                                    }
                            )
                    )
                } else {
                    safeStartCall(isVideoCall)
                }
            }
            else -> {
                // it's jitsi call
                // can you add widgets??
                if (!state.isAllowedToManageWidgets) {
                    // You do not have permission to start a conference call in this room
                    showDialogWithMessage(
                            fragment.getString(
                                    if (state.isDm()) {
                                        CommonStrings.no_permissions_to_start_conf_call_in_direct_room
                                    } else {
                                        CommonStrings.no_permissions_to_start_conf_call
                                    }
                            )
                    )
                } else {
                    if (state.hasActiveJitsiWidget()) {
                        // A conference is already in progress, return
                    } else {
                        MaterialAlertDialogBuilder(fragment.requireContext())
                                .setTitle(if (isVideoCall) CommonStrings.video_meeting else CommonStrings.audio_meeting)
                                .setMessage(CommonStrings.audio_video_meeting_description)
                                .setPositiveButton(fragment.getString(CommonStrings.create)) { _, _ ->
                                    // create the widget, then navigate to it..
                                    timelineViewModel.handle(RoomDetailAction.AddJitsiWidget(isVideoCall))
                                }
                                .setNegativeButton(fragment.getString(CommonStrings.action_cancel), null)
                                .show()
                    }
                }
            }
        }
    }

    private fun safeStartCall(isVideoCall: Boolean) {
        if (vectorPreferences.preventAccidentalCall()) {
            MaterialAlertDialogBuilder(fragment.requireActivity())
                    .setMessage(if (isVideoCall) CommonStrings.start_video_call_prompt_msg else CommonStrings.start_voice_call_prompt_msg)
                    .setPositiveButton(if (isVideoCall) CommonStrings.start_video_call else CommonStrings.start_voice_call) { _, _ ->
                        safeStartCall2(isVideoCall)
                    }
                    .setNegativeButton(CommonStrings.action_cancel, null)
                    .show()
        } else {
            safeStartCall2(isVideoCall)
        }
    }

    private fun safeStartCall2(isVideoCall: Boolean) {
        val startCallAction = RoomDetailAction.StartCall(isVideoCall)
        timelineViewModel.pendingAction = startCallAction
        if (isVideoCall) {
            if (checkPermissions(
                            PERMISSIONS_FOR_VIDEO_IP_CALL,
                            fragment.requireActivity(),
                            startCallActivityResultLauncher,
                            CommonStrings.permissions_rationale_msg_camera_and_audio
                    )) {
                timelineViewModel.pendingAction = null
                timelineViewModel.handle(startCallAction)
            }
        } else {
            if (checkPermissions(
                            PERMISSIONS_FOR_AUDIO_IP_CALL,
                            fragment.requireActivity(),
                            startCallActivityResultLauncher,
                            CommonStrings.permissions_rationale_msg_record_audio
                    )) {
                timelineViewModel.pendingAction = null
                timelineViewModel.handle(startCallAction)
            }
        }
    }
}
