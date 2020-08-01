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

import android.view.View
import im.vector.matrix.android.api.session.room.members.ChangeMembershipState
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.riotx.R
import im.vector.riotx.core.date.VectorDateFormatter
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import im.vector.riotx.core.extensions.localDateTime
import im.vector.riotx.core.resources.DateProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.utils.DebouncedClickListener
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.timeline.format.DisplayableEventFormatter
import im.vector.riotx.features.home.room.typing.TypingHelper
import javax.inject.Inject

class RoomSummaryItemFactory @Inject constructor(private val displayableEventFormatter: DisplayableEventFormatter,
                                                 private val dateFormatter: VectorDateFormatter,
                                                 private val stringProvider: StringProvider,
                                                 private val typingHelper: TypingHelper,
                                                 private val avatarRenderer: AvatarRenderer) {

    fun create(roomSummary: RoomSummary,
               roomChangeMembershipStates: Map<String, ChangeMembershipState>,
               selectedRoomIds: Set<String>,
               listener: RoomSummaryController.Listener?): VectorEpoxyModel<*> {
        return when (roomSummary.membership) {
            Membership.INVITE -> {
                val changeMembershipState = roomChangeMembershipStates[roomSummary.roomId] ?: ChangeMembershipState.Unknown
                createInvitationItem(roomSummary, changeMembershipState, listener)
            }
            else              -> createRoomItem(roomSummary, selectedRoomIds, listener?.let { it::onRoomClicked }, listener?.let { it::onRoomLongClicked })
        }
    }

    private fun createInvitationItem(roomSummary: RoomSummary,
                             changeMembershipState: ChangeMembershipState,
                             listener: RoomSummaryController.Listener?): VectorEpoxyModel<*> {
        val secondLine = if (roomSummary.isDirect) {
            roomSummary.inviterId
        } else {
            roomSummary.inviterId?.let {
                stringProvider.getString(R.string.invited_by, it)
            }
        }

        return RoomInvitationItem_()
                .id(roomSummary.roomId)
                .avatarRenderer(avatarRenderer)
                .matrixItem(roomSummary.toMatrixItem())
                .secondLine(secondLine)
                .changeMembershipState(changeMembershipState)
                .acceptListener { listener?.onAcceptRoomInvitation(roomSummary) }
                .rejectListener { listener?.onRejectRoomInvitation(roomSummary) }
                .listener { listener?.onRoomClicked(roomSummary) }
    }

    fun createRoomItem(
            roomSummary: RoomSummary,
            selectedRoomIds: Set<String>,
            onClick: ((RoomSummary) -> Unit)?,
            onLongClick: ((RoomSummary) -> Boolean)?
    ): VectorEpoxyModel<*> {
        val unreadCount = roomSummary.notificationCount
        val showHighlighted = roomSummary.highlightCount > 0
        val showSelected = selectedRoomIds.contains(roomSummary.roomId)
        var latestFormattedEvent: CharSequence = ""
        var latestEventTime: CharSequence = ""
        val latestEvent = roomSummary.latestPreviewableEvent
        if (latestEvent != null) {
            val date = latestEvent.root.localDateTime()
            val currentDate = DateProvider.currentLocalDateTime()
            val isSameDay = date.toLocalDate() == currentDate.toLocalDate()
            latestFormattedEvent = displayableEventFormatter.format(latestEvent, roomSummary.isDirect.not())
            latestEventTime = if (isSameDay) {
                dateFormatter.formatMessageHour(date)
            } else {
                dateFormatter.formatMessageDay(date)
            }
        }
        val typingMessage = typingHelper.getTypingMessage(roomSummary.typingUsers)
        return RoomSummaryItem_()
                .id(roomSummary.roomId)
                .avatarRenderer(avatarRenderer)
                .encryptionTrustLevel(roomSummary.roomEncryptionTrustLevel)
                .matrixItem(roomSummary.toMatrixItem())
                .lastEventTime(latestEventTime)
                .typingMessage(typingMessage)
                .lastEvent(latestFormattedEvent.toString())
                .lastFormattedEvent(latestFormattedEvent)
                .showHighlighted(showHighlighted)
                .showSelected(showSelected)
                .hasFailedSending(roomSummary.hasFailedSending)
                .unreadNotificationCount(unreadCount)
                .hasUnreadMessage(roomSummary.hasUnreadMessages)
                .hasDraft(roomSummary.userDrafts.isNotEmpty())
                .itemLongClickListener { _ ->
                    onLongClick?.invoke(roomSummary) ?: false
                }
                .itemClickListener(
                        DebouncedClickListener(View.OnClickListener { _ ->
                            onClick?.invoke(roomSummary)
                        })
                )
    }
}
