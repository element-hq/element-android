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

package im.vector.riotx.features.home.room.list

import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import im.vector.riotx.core.extensions.localDateTime
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.resources.DateProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.timeline.format.NoticeEventFormatter
import im.vector.riotx.core.date.VectorDateFormatter
import im.vector.riotx.features.home.room.detail.timeline.helper.senderName
import me.gujun.android.span.span
import javax.inject.Inject

class RoomSummaryItemFactory @Inject constructor(private val noticeEventFormatter: NoticeEventFormatter,
                                                 private val dateFormatter: VectorDateFormatter,
                                                 private val colorProvider: ColorProvider,
                                                 private val stringProvider: StringProvider,
                                                 private val avatarRenderer: AvatarRenderer,
                                                 private val session: Session) {

    fun create(roomSummary: RoomSummary,
               joiningRoomsIds: Set<String>,
               joiningErrorRoomsIds: Set<String>,
               rejectingRoomsIds: Set<String>,
               rejectingErrorRoomsIds: Set<String>,
               listener: RoomSummaryController.Listener?): VectorEpoxyModel<*> {
        return when (roomSummary.membership) {
            Membership.INVITE -> createInvitationItem(roomSummary, joiningRoomsIds, joiningErrorRoomsIds, rejectingRoomsIds, rejectingErrorRoomsIds, listener)
            else              -> createRoomItem(roomSummary, listener)
        }
    }

    private fun createInvitationItem(roomSummary: RoomSummary,
                                     joiningRoomsIds: Set<String>,
                                     joiningErrorRoomsIds: Set<String>,
                                     rejectingRoomsIds: Set<String>,
                                     rejectingErrorRoomsIds: Set<String>,
                                     listener: RoomSummaryController.Listener?): VectorEpoxyModel<*> {
        val secondLine = if (roomSummary.isDirect) {
            roomSummary.latestPreviewableEvent?.root?.senderId
        } else {
            roomSummary.latestPreviewableEvent?.root?.senderId?.let {
                stringProvider.getString(R.string.invited_by, it)
            }
        }

        return RoomInvitationItem_()
                .id(roomSummary.roomId)
                .avatarRenderer(avatarRenderer)
                .roomId(roomSummary.roomId)
                .secondLine(secondLine)
                .invitationAcceptInProgress(joiningRoomsIds.contains(roomSummary.roomId))
                .invitationAcceptInError(joiningErrorRoomsIds.contains(roomSummary.roomId))
                .invitationRejectInProgress(rejectingRoomsIds.contains(roomSummary.roomId))
                .invitationRejectInError(rejectingErrorRoomsIds.contains(roomSummary.roomId))
                .acceptListener { listener?.onAcceptRoomInvitation(roomSummary) }
                .rejectListener { listener?.onRejectRoomInvitation(roomSummary) }
                .roomName(roomSummary.displayName)
                .avatarUrl(roomSummary.avatarUrl)
                .listener { listener?.onRoomSelected(roomSummary) }
    }

    private fun createRoomItem(roomSummary: RoomSummary, listener: RoomSummaryController.Listener?): VectorEpoxyModel<*> {
        val unreadCount = roomSummary.notificationCount
        val showHighlighted = roomSummary.highlightCount > 0

        var latestFormattedEvent: CharSequence = ""
        var latestEventTime: CharSequence = ""
        val latestEvent = roomSummary.latestPreviewableEvent
        if (latestEvent != null) {
            val date = latestEvent.root.localDateTime()
            val currentDate = DateProvider.currentLocalDateTime()
            val isSameDay = date.toLocalDate() == currentDate.toLocalDate()
            latestFormattedEvent = if (latestEvent.root.isEncrypted()
                                       && latestEvent.root.mxDecryptionResult == null) {
                stringProvider.getString(R.string.encrypted_message)
            } else if (latestEvent.root.getClearType() == EventType.MESSAGE) {
                val senderName = latestEvent.senderName() ?: latestEvent.root.senderId
                val content = latestEvent.root.getClearContent()?.toModel<MessageContent>()
                val message = content?.body ?: ""
                if (roomSummary.isDirect.not() && senderName != null) {
                    span {
                        text = senderName
                        textColor = colorProvider.getColorFromAttribute(R.attr.riotx_text_primary)
                    }
                            .append(" - ")
                            .append(message)
                } else {
                    message
                }
            } else {
                span {
                    text = noticeEventFormatter.format(latestEvent) ?: ""
                    textStyle = "italic"
                }
            }
            latestEventTime = if (isSameDay) {
                dateFormatter.formatMessageHour(date)
            } else {
                dateFormatter.formatMessageDay(date)
            }
        }
        return RoomSummaryItem_()
                .id(roomSummary.roomId)
                .avatarRenderer(avatarRenderer)
                .roomId(roomSummary.roomId)
                .lastEventTime(latestEventTime)
                .lastFormattedEvent(latestFormattedEvent)
                .roomName(roomSummary.displayName)
                .avatarUrl(roomSummary.avatarUrl)
                .showHighlighted(showHighlighted)
                .unreadNotificationCount(unreadCount)
                .hasUnreadMessage(roomSummary.hasUnreadMessages)
                .listener { listener?.onRoomSelected(roomSummary) }
    }

}
