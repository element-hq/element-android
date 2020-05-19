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
 */

package im.vector.riotx.features.home.room.detail

import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.model.message.MessageFileContent
import im.vector.matrix.android.api.session.room.model.message.MessageStickerContent
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotx.core.platform.VectorViewModelAction

sealed class RoomDetailAction : VectorViewModelAction {
    data class UserIsTyping(val isTyping: Boolean) : RoomDetailAction()
    data class SaveDraft(val draft: String) : RoomDetailAction()
    data class SendSticker(val stickerContent: MessageStickerContent) : RoomDetailAction()
    data class SendMessage(val text: CharSequence, val autoMarkdown: Boolean) : RoomDetailAction()
    data class SendMedia(val attachments: List<ContentAttachmentData>, val compressBeforeSending: Boolean) : RoomDetailAction()
    data class TimelineEventTurnsVisible(val event: TimelineEvent) : RoomDetailAction()
    data class TimelineEventTurnsInvisible(val event: TimelineEvent) : RoomDetailAction()
    data class LoadMoreTimelineEvents(val direction: Timeline.Direction) : RoomDetailAction()
    data class SendReaction(val targetEventId: String, val reaction: String) : RoomDetailAction()
    data class UndoReaction(val targetEventId: String, val reaction: String, val reason: String? = "") : RoomDetailAction()
    data class RedactAction(val targetEventId: String, val reason: String? = "") : RoomDetailAction()
    data class UpdateQuickReactAction(val targetEventId: String, val selectedReaction: String, val add: Boolean) : RoomDetailAction()
    data class NavigateToEvent(val eventId: String, val highlight: Boolean) : RoomDetailAction()
    object MarkAllAsRead : RoomDetailAction()
    data class DownloadFile(val eventId: String, val messageFileContent: MessageFileContent) : RoomDetailAction()
    data class HandleTombstoneEvent(val event: Event) : RoomDetailAction()
    object AcceptInvite : RoomDetailAction()
    object RejectInvite : RoomDetailAction()

    object EnterTrackingUnreadMessagesState : RoomDetailAction()
    object ExitTrackingUnreadMessagesState : RoomDetailAction()

    data class EnterEditMode(val eventId: String, val text: String) : RoomDetailAction()
    data class EnterQuoteMode(val eventId: String, val text: String) : RoomDetailAction()
    data class EnterReplyMode(val eventId: String, val text: String) : RoomDetailAction()
    data class ExitSpecialMode(val text: String) : RoomDetailAction()

    data class ResendMessage(val eventId: String) : RoomDetailAction()
    data class RemoveFailedEcho(val eventId: String) : RoomDetailAction()

    data class ReplyToOptions(val eventId: String, val optionIndex: Int, val optionValue: String) : RoomDetailAction()

    data class ReportContent(
            val eventId: String,
            val senderId: String?,
            val reason: String,
            val spam: Boolean = false,
            val inappropriate: Boolean = false) : RoomDetailAction()

    data class IgnoreUser(val userId: String?) : RoomDetailAction()

    object ClearSendQueue : RoomDetailAction()
    object ResendAll : RoomDetailAction()
    data class StartCall(val isVideo: Boolean) : RoomDetailAction()
    object EndCall : RoomDetailAction()

    data class AcceptVerificationRequest(val transactionId: String, val otherUserId: String) : RoomDetailAction()
    data class DeclineVerificationRequest(val transactionId: String, val otherUserId: String) : RoomDetailAction()
    data class RequestVerification(val userId: String) : RoomDetailAction()
    data class ResumeVerification(val transactionId: String, val otherUserId: String?) : RoomDetailAction()
    data class TapOnFailedToDecrypt(val eventId: String) : RoomDetailAction()
    data class ReRequestKeys(val eventId: String) : RoomDetailAction()

    object SelectStickerAttachment : RoomDetailAction()
    object OpenIntegrationManager: RoomDetailAction()
}
