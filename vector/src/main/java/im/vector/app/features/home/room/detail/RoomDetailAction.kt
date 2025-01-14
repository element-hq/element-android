/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail

import android.net.Uri
import android.view.View
import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.call.conference.ConferenceEvent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.room.model.message.MessageStickerContent
import org.matrix.android.sdk.api.session.room.model.message.MessageWithAttachmentContent
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.util.MatrixItem

sealed class RoomDetailAction : VectorViewModelAction {
    data class SendSticker(val stickerContent: MessageStickerContent) : RoomDetailAction()
    data class SendMedia(val attachments: List<ContentAttachmentData>, val compressBeforeSending: Boolean) : RoomDetailAction()
    data class TimelineEventTurnsVisible(val event: TimelineEvent) : RoomDetailAction()
    data class TimelineEventTurnsInvisible(val event: TimelineEvent) : RoomDetailAction()
    data class LoadMoreTimelineEvents(val direction: Timeline.Direction) : RoomDetailAction()
    data class SendReaction(val targetEventId: String, val reaction: String) : RoomDetailAction()
    data class UndoReaction(val targetEventId: String, val reaction: String, val reason: String? = "") : RoomDetailAction()
    data class RedactAction(val targetEventId: String, val reason: String? = "") : RoomDetailAction()
    data class UpdateQuickReactAction(val targetEventId: String, val selectedReaction: String, val add: Boolean) : RoomDetailAction()
    data class NavigateToEvent(val eventId: String, val highlight: Boolean, val isFirstUnreadEvent: Boolean = false) : RoomDetailAction()
    object MarkAllAsRead : RoomDetailAction()
    data class DownloadOrOpen(val eventId: String, val senderId: String?, val messageFileContent: MessageWithAttachmentContent) : RoomDetailAction()
    object JoinAndOpenReplacementRoom : RoomDetailAction()
    object OnClickMisconfiguredEncryption : RoomDetailAction()
    object AcceptInvite : RoomDetailAction()
    object RejectInvite : RoomDetailAction()

    object EnterTrackingUnreadMessagesState : RoomDetailAction()
    object ExitTrackingUnreadMessagesState : RoomDetailAction()

    data class ResendMessage(val eventId: String) : RoomDetailAction()
    data class RemoveFailedEcho(val eventId: String) : RoomDetailAction()
    data class CancelSend(val event: TimelineEvent, val force: Boolean) : RoomDetailAction()

    data class VoteToPoll(val eventId: String, val optionKey: String) : RoomDetailAction()

    data class ReportContent(
            val eventId: String,
            val senderId: String?,
            val reason: String,
            val spam: Boolean = false,
            val inappropriate: Boolean = false,
            val user: Boolean = false,
    ) : RoomDetailAction()

    data class IgnoreUser(val userId: String?) : RoomDetailAction()

    object ResendAll : RoomDetailAction()

    data class StartCall(val isVideo: Boolean) : RoomDetailAction()
    data class AcceptCall(val callId: String) : RoomDetailAction()
    object EndCall : RoomDetailAction()

    data class AcceptVerificationRequest(val transactionId: String, val otherUserId: String) : RoomDetailAction()
    data class DeclineVerificationRequest(val transactionId: String, val otherUserId: String) : RoomDetailAction()
    data class RequestVerification(val userId: String) : RoomDetailAction()
    data class ResumeVerification(val transactionId: String, val otherUserId: String?) : RoomDetailAction()
    data class TapOnFailedToDecrypt(val eventId: String) : RoomDetailAction()
    data class ReRequestKeys(val eventId: String) : RoomDetailAction()

    object SelectStickerAttachment : RoomDetailAction()
    object OpenIntegrationManager : RoomDetailAction()
    object ManageIntegrations : RoomDetailAction()
    data class AddJitsiWidget(val withVideo: Boolean) : RoomDetailAction()
    data class RemoveWidget(val widgetId: String) : RoomDetailAction()

    object JoinJitsiCall : RoomDetailAction()
    object LeaveJitsiCall : RoomDetailAction()

    data class EnsureNativeWidgetAllowed(
            val widget: Widget,
            val userJustAccepted: Boolean,
            val grantedEvents: RoomDetailViewEvents
    ) : RoomDetailAction()

    data class UpdateJoinJitsiCallStatus(val conferenceEvent: ConferenceEvent) : RoomDetailAction()

    data class JumpToReadReceipt(val userId: String) : RoomDetailAction()
    object QuickActionInvitePeople : RoomDetailAction()
    object QuickActionSetAvatar : RoomDetailAction()
    data class SetAvatarAction(val newAvatarUri: Uri, val newAvatarFileName: String) : RoomDetailAction()
    object QuickActionSetTopic : RoomDetailAction()
    data class ShowRoomAvatarFullScreen(val matrixItem: MatrixItem?, val transitionView: View?) : RoomDetailAction()

    // Preview URL
    data class DoNotShowPreviewUrlFor(val eventId: String, val url: String) : RoomDetailAction()

    data class ComposerFocusChange(val focused: Boolean) : RoomDetailAction()

    // Failed messages
    object RemoveAllFailedMessages : RoomDetailAction()

    data class RoomUpgradeSuccess(val replacementRoomId: String) : RoomDetailAction()

    // Poll
    data class EndPoll(val eventId: String) : RoomDetailAction()

    // Live Location
    object StopLiveLocationSharing : RoomDetailAction()

    object OpenElementCallWidget : RoomDetailAction()

    sealed class VoiceBroadcastAction : RoomDetailAction() {
        sealed class Recording : VoiceBroadcastAction() {
            object Start : Recording()
            object Pause : Recording()
            object Resume : Recording()
            object Stop : Recording()
            object StopConfirmed : Recording()
        }

        sealed class Listening : VoiceBroadcastAction() {
            data class PlayOrResume(val voiceBroadcast: VoiceBroadcast) : Listening()
            object Pause : Listening()
            object Stop : Listening()
            data class SeekTo(val voiceBroadcast: VoiceBroadcast, val positionMillis: Int, val duration: Int) : Listening()
        }
    }
}
