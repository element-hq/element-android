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

package im.vector.riotredesign.features.home.room.list

import androidx.annotation.StringRes
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.riotredesign.core.extensions.localDateTime
import im.vector.riotredesign.core.resources.DateProvider
import im.vector.riotredesign.core.resources.StringProvider
import im.vector.riotredesign.features.home.AvatarRenderer
import im.vector.riotredesign.features.home.room.detail.timeline.format.NoticeEventFormatter
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineDateFormatter
import javax.inject.Inject

class RoomSummaryController @Inject constructor(private val stringProvider: StringProvider,
                                                private val eventFormatter: NoticeEventFormatter,
                                                private val timelineDateFormatter: TimelineDateFormatter,
                                                private val avatarRenderer: AvatarRenderer
) : TypedEpoxyController<RoomListViewState>() {

    var callback: Callback? = null

    override fun buildModels(viewState: RoomListViewState) {
        val roomSummaries = viewState.asyncFilteredRooms()
        roomSummaries?.forEach { (category, summaries) ->
            if (summaries.isEmpty()) {
                return@forEach
            } else {
                val isExpanded = viewState.isCategoryExpanded(category)
                buildRoomCategory(viewState, summaries, category.titleRes, viewState.isCategoryExpanded(category)) {
                    callback?.onToggleRoomCategory(category)
                }
                if (isExpanded) {
                    buildRoomModels(summaries)
                }
            }
        }
    }

    private fun buildRoomCategory(viewState: RoomListViewState,
                                  summaries: List<RoomSummary>,
                                  @StringRes titleRes: Int,
                                  isExpanded: Boolean,
                                  mutateExpandedState: () -> Unit) {
        if (summaries.isEmpty()) {
            return
        }
        //TODO should add some business logic later
        val unreadCount = if (summaries.isEmpty()) {
            0
        } else {
            summaries.map { it.notificationCount }.sumBy { i -> i }
        }
        val showHighlighted = summaries.any { it.highlightCount > 0 }
        roomCategoryItem {
            id(titleRes)
            title(stringProvider.getString(titleRes).toUpperCase())
            expanded(isExpanded)
            unreadCount(unreadCount)
            showHighlighted(showHighlighted)
            listener {
                mutateExpandedState()
                setData(viewState)
            }
        }
    }

    private fun buildRoomModels(summaries: List<RoomSummary>) {
        summaries.forEach { roomSummary ->
            val unreadCount = roomSummary.notificationCount
            val showHighlighted = roomSummary.highlightCount > 0

            var lastMessageFormatted: CharSequence = ""
            var lastMessageTime: CharSequence = ""
            val lastMessage = roomSummary.lastMessage
            if (lastMessage != null) {
                val date = lastMessage.localDateTime()
                val currentData = DateProvider.currentLocalDateTime()
                val isSameDay = date.toLocalDate() == currentData.toLocalDate()
                //TODO: get formatted
                if (lastMessage.type == EventType.MESSAGE) {
                    val content = lastMessage.content?.toModel<MessageContent>()
                    lastMessageFormatted = content?.body ?: ""
                } else {
                    lastMessageFormatted = lastMessage.type
                }
                lastMessageTime = if (isSameDay) {
                    timelineDateFormatter.formatMessageHour(date)
                } else {
                    //TODO: change this
                    timelineDateFormatter.formatMessageDay(date)
                }


            }
            roomSummaryItem {
                avatarRenderer(avatarRenderer)
                id(roomSummary.roomId)
                roomId(roomSummary.roomId)
                lastEventTime(lastMessageTime)
                lastFormattedEvent(lastMessageFormatted)
                roomName(roomSummary.displayName)
                avatarUrl(roomSummary.avatarUrl)
                showHighlighted(showHighlighted)
                unreadCount(unreadCount)
                listener { callback?.onRoomSelected(roomSummary) }
            }
        }
    }

    interface Callback {
        fun onToggleRoomCategory(roomCategory: RoomCategory)
        fun onRoomSelected(room: RoomSummary)
    }

}
