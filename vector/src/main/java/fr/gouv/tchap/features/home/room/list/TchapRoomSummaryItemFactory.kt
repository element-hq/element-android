/*
 * Copyright 2021 New Vector Ltd
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

package fr.gouv.tchap.features.home.room.list

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import fr.gouv.tchap.core.utils.RoomUtils
import fr.gouv.tchap.core.utils.TchapUtils
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.DebouncedClickListener
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.format.DisplayableEventFormatter
import im.vector.app.features.home.room.list.RoomInvitationItem_
import im.vector.app.features.home.room.list.RoomListListener
import im.vector.app.features.home.room.list.SpaceChildInfoItem_
import im.vector.app.features.home.room.typing.TypingHelper
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class TchapRoomSummaryItemFactory @Inject constructor(private val displayableEventFormatter: DisplayableEventFormatter,
                                                      private val dateFormatter: VectorDateFormatter,
                                                      private val stringProvider: StringProvider,
                                                      private val typingHelper: TypingHelper,
                                                      private val avatarRenderer: AvatarRenderer,
                                                      private val session: Session) {

    fun create(roomSummary: RoomSummary,
               roomChangeMembershipStates: Map<String, ChangeMembershipState>,
               selectedRoomIds: Set<String>,
               listener: RoomListListener?): VectorEpoxyModel<*> {
        return when (roomSummary.membership) {
            Membership.INVITE -> {
                val changeMembershipState = roomChangeMembershipStates[roomSummary.roomId] ?: ChangeMembershipState.Unknown
                createInvitationItem(roomSummary, changeMembershipState, listener)
            }
            else              -> createRoomItem(roomSummary, selectedRoomIds, listener?.let { it::onRoomClicked }, listener?.let { it::onRoomLongClicked })
        }
    }

    fun createSuggestion(spaceChildInfo: SpaceChildInfo,
                         suggestedRoomJoiningStates: Map<String, Async<Unit>>,
                         listener: RoomListListener?): VectorEpoxyModel<*> {
        return SpaceChildInfoItem_()
                .id("sug_${spaceChildInfo.childRoomId}")
                .matrixItem(spaceChildInfo.toMatrixItem())
                .avatarRenderer(avatarRenderer)
                .topic(spaceChildInfo.topic)
                .buttonLabel(stringProvider.getString(R.string.join))
                .loading(suggestedRoomJoiningStates[spaceChildInfo.childRoomId] is Loading)
                .memberCount(spaceChildInfo.activeMemberCount ?: 0)
                .buttonClickListener(DebouncedClickListener({ listener?.onJoinSuggestedRoom(spaceChildInfo) }))
                .itemClickListener(DebouncedClickListener({ listener?.onSuggestedRoomClicked(spaceChildInfo) }))
    }

    private fun createInvitationItem(roomSummary: RoomSummary,
                                     changeMembershipState: ChangeMembershipState,
                                     listener: RoomListListener?): VectorEpoxyModel<*> {
        val secondLine = roomSummary.inviterId?.let { userId ->
            val displayName = session.getUser(userId)?.displayName
                    ?.let { displayName ->
                        displayName.takeUnless { roomSummary.isDirect } ?: TchapUtils.getNameFromDisplayName(displayName)
                    }
                    ?: TchapUtils.computeDisplayNameFromUserId(userId)
                    ?: userId
            stringProvider.getString(R.string.tchap_room_invited_you, displayName)
        }

        return RoomInvitationItem_()
                .id(roomSummary.roomId)
                .avatarRenderer(avatarRenderer)
                .matrixItem(roomSummary.toMatrixItem())
                .secondLine(secondLine)
                .isDirect(roomSummary.isDirect)
                .changeMembershipState(changeMembershipState)
                .acceptListener { listener?.onAcceptRoomInvitation(roomSummary) }
                .rejectListener { listener?.onRejectRoomInvitation(roomSummary) }
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
            latestFormattedEvent = displayableEventFormatter.format(latestEvent, roomSummary.isDirect.not())
            latestEventTime = dateFormatter.format(latestEvent.root.originServerTs, DateFormatKind.ROOM_LIST)
        }
        val typingMessage = typingHelper.getTypingMessage(roomSummary.typingUsers)
        return TchapRoomSummaryItem_()
                .id(roomSummary.roomId)
                .avatarRenderer(avatarRenderer)
                // We do not display shield in the room list anymore
                // .encryptionTrustLevel(roomSummary.roomEncryptionTrustLevel)
                .matrixItem(roomSummary.toMatrixItem())
                .isDirect(roomSummary.isDirect)
                .isEncrypted(roomSummary.isEncrypted)
                .isPinned(roomSummary.isFavorite)
                .roomType(RoomUtils.getRoomType(roomSummary))
                .lastEventTime(latestEventTime)
                .typingMessage(typingMessage)
                .lastEvent(latestFormattedEvent.toString())
                .lastFormattedEvent(latestFormattedEvent)
                .showHighlighted(showHighlighted)
                .showSelected(showSelected)
                .hasFailedSending(roomSummary.hasFailedSending)
                .unreadNotificationCount(unreadCount)
                .hasUnreadMessage(roomSummary.hasUnreadMessages)
                // FIXME: Check if room has disabled notifications
                .hasDisabledNotifications(false)
                // FIXME: Check if user has expected actions
                .hasExpectedAction(false)
                .hasDraft(roomSummary.userDrafts.isNotEmpty())
                .itemLongClickListener { _ ->
                    onLongClick?.invoke(roomSummary) ?: false
                }
                .itemClickListener(
                        DebouncedClickListener({
                            onClick?.invoke(roomSummary)
                        })
                )
    }
}
