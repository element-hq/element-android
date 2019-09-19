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

import com.jaiselrahman.filepicker.model.MediaFile
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.model.message.MessageFileContent
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent

sealed class RoomDetailActions {

    data class SaveDraft(val draft: String) : RoomDetailActions()
    data class SendMessage(val text: String, val autoMarkdown: Boolean) : RoomDetailActions()
    data class SendMedia(val mediaFiles: List<MediaFile>) : RoomDetailActions()
    data class EventDisplayed(val event: TimelineEvent) : RoomDetailActions()
    data class LoadMoreTimelineEvents(val direction: Timeline.Direction) : RoomDetailActions()
    data class SendReaction(val reaction: String, val targetEventId: String) : RoomDetailActions()
    data class RedactAction(val targetEventId: String, val reason: String? = "") : RoomDetailActions()
    data class UndoReaction(val targetEventId: String, val key: String, val reason: String? = "") : RoomDetailActions()
    data class UpdateQuickReactAction(val targetEventId: String, val selectedReaction: String, val add: Boolean) : RoomDetailActions()
    data class NavigateToEvent(val eventId: String, val position: Int?) : RoomDetailActions()
    data class DownloadFile(val eventId: String, val messageFileContent: MessageFileContent) : RoomDetailActions()
    data class HandleTombstoneEvent(val event: Event) : RoomDetailActions()
    object AcceptInvite : RoomDetailActions()
    object RejectInvite : RoomDetailActions()

    data class EnterEditMode(val eventId: String, val draft: String) : RoomDetailActions()
    data class EnterQuoteMode(val eventId: String, val draft: String) : RoomDetailActions()
    data class EnterReplyMode(val eventId: String, val draft: String) : RoomDetailActions()
    data class ExitSpecialMode(val draft: String) : RoomDetailActions()

    data class ResendMessage(val eventId: String) : RoomDetailActions()
    data class RemoveFailedEcho(val eventId: String) : RoomDetailActions()
    object ClearSendQueue : RoomDetailActions()
    object ResendAll : RoomDetailActions()


}