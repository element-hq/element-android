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

package im.vector.app.features.home.room.list

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.format.DisplayableEventFormatter
import im.vector.app.features.home.room.typing.TypingHelper
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class RoomSummaryItemFactory @Inject constructor(private val displayableEventFormatter: DisplayableEventFormatter,
                                                 private val dateFormatter: VectorDateFormatter,
                                                 private val stringProvider: StringProvider,
                                                 private val typingHelper: TypingHelper,
                                                 private val avatarRenderer: AvatarRenderer,
                                                 private val errorFormatter: ErrorFormatter) {

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
        val error = (suggestedRoomJoiningStates[spaceChildInfo.childRoomId] as? Fail)?.error
        return SpaceChildInfoItem_()
                .id("sug_${spaceChildInfo.childRoomId}")
                .matrixItem(spaceChildInfo.toMatrixItem())
                .avatarRenderer(avatarRenderer)
                .topic(spaceChildInfo.topic)
                .errorLabel(
                        error?.let {
                            stringProvider.getString(R.string.error_failed_to_join_room, errorFormatter.toHumanReadable(it))
                        }
                )
                .buttonLabel(
                        if (error != null) stringProvider.getString(R.string.global_retry)
                        else stringProvider.getString(R.string.join)
                )
                .loading(suggestedRoomJoiningStates[spaceChildInfo.childRoomId] is Loading)
                .memberCount(spaceChildInfo.activeMemberCount ?: 0)
                .buttonClickListener { listener?.onJoinSuggestedRoom(spaceChildInfo) }
                .itemClickListener { listener?.onSuggestedRoomClicked(spaceChildInfo) }
    }

    private fun createInvitationItem(roomSummary: RoomSummary,
                                     changeMembershipState: ChangeMembershipState,
                                     listener: RoomListListener?): VectorEpoxyModel<*> {
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
            latestFormattedEvent = displayableEventFormatter.format(latestEvent, roomSummary.isDirect, roomSummary.isDirect.not())
            latestEventTime = dateFormatter.format(latestEvent.root.originServerTs, DateFormatKind.ROOM_LIST)
        }
        val typingMessage = typingHelper.getTypingMessage(roomSummary.typingUsers)
        return RoomSummaryItem_()
                .id(roomSummary.roomId)
                .avatarRenderer(avatarRenderer)
                // We do not display shield in the room list anymore
                // .encryptionTrustLevel(roomSummary.roomEncryptionTrustLevel)
                .izPublic(roomSummary.isPublic)
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
                .itemClickListener { onClick?.invoke(roomSummary) }
    }
}
