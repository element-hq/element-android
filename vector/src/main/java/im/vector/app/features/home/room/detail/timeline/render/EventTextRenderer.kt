/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.detail.timeline.render

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.util.Patterns
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.glide.GlideApp
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.html.PillImageSpan
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.getUser
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem

class EventTextRenderer @AssistedInject constructor(
        @Assisted private val roomId: String?,
        private val context: Context,
        private val avatarRenderer: AvatarRenderer,
        private val activeSessionHolder: ActiveSessionHolder,
) {

    private val urlRegex = Patterns.WEB_URL.toRegex()

    @AssistedFactory
    interface Factory {
        fun create(roomId: String?): EventTextRenderer
    }

    /**
     * @param text the text to be rendered
     */
    fun render(text: CharSequence): CharSequence {
        return renderNotifyEveryone(renderPermalinks(text))
//        return renderNotifyEveryone(text)
    }

    private fun renderNotifyEveryone(text: CharSequence): CharSequence {
        return if (roomId != null && text.contains(MatrixItem.NOTIFY_EVERYONE)) {
            SpannableStringBuilder(text).apply {
                addNotifyEveryoneSpans(this, roomId)
            }
        } else {
            text
        }
    }

    private fun renderPermalinks(text: CharSequence): CharSequence {
        return if (roomId != null) {
            SpannableStringBuilder(text).apply {
                addPermalinksSpans(this)
            }
        } else {
            text
        }
    }

    private fun addNotifyEveryoneSpans(text: Spannable, roomId: String) {
        val room: RoomSummary? = activeSessionHolder.getSafeActiveSession()?.roomService()?.getRoomSummary(roomId)
        val matrixItem = MatrixItem.EveryoneInRoomItem(
                id = roomId,
                avatarUrl = room?.avatarUrl,
                roomDisplayName = room?.displayName
        )

        // search for notify everyone text
        var foundIndex = text.indexOf(MatrixItem.NOTIFY_EVERYONE, 0)
        while (foundIndex >= 0) {
            val endSpan = foundIndex + MatrixItem.NOTIFY_EVERYONE.length
            addPillSpan(text, createPillImageSpan(matrixItem), foundIndex, endSpan)
            foundIndex = text.indexOf(MatrixItem.NOTIFY_EVERYONE, endSpan)
        }
    }

    private fun addPermalinksSpans(text: Spannable) {
        val session = activeSessionHolder.getSafeActiveSession()

        for (pattern in listOf(urlRegex).plus(MatrixPatterns.MATRIX_PATTERNS)) {
            for (match in pattern.findAll(text)) {
                val startPos = match.range.first
                if (startPos == 0 || text[startPos - 1] != '/') {
                    val endPos = match.range.last + 1
                    val url = text.substring(match.range)
                    val matrixItem = if (MatrixPatterns.isPermalink(url)) {
                        when (val permalinkData = PermalinkParser.parse(url)) {
                            is PermalinkData.RoomLink -> createMatrixItem(permalinkData.roomIdOrAlias, permalinkData.eventId, session)
                            is PermalinkData.UserLink -> createMatrixItem(permalinkData.userId, null, session)
                            else -> {
                                null
                            }
                        }
                    } else if (MatrixPatterns.isUserId(url) ||
                            MatrixPatterns.isRoomAlias(url) ||
                            MatrixPatterns.isRoomId(url) ||
                            MatrixPatterns.isGroupId(url) ||
                            MatrixPatterns.isEventId(url)) {
                        createMatrixItem(url, null, session)
                    } else null

                    if (matrixItem != null) {
                        addPillSpan(text, createPillImageSpan(matrixItem), startPos, endPos)
                    }
                }
            }
        }
    }

    private fun createMatrixItem(matrixId: String, eventId: String?, session: Session?): MatrixItem? {
        return when {
            MatrixPatterns.isUserId(matrixId) -> {
                session?.getUser(matrixId)?.toMatrixItem() ?: MatrixItem.UserItem(matrixId, context.getString(R.string.pill_message_unknown_user))
            }
            !eventId.isNullOrEmpty() -> {
                if (matrixId == roomId) {
                    val room = session?.getRoom(matrixId)
                    val event = room?.getTimelineEvent(eventId)
                    val user = event?.senderInfo?.userId?.let { room.membershipService().getRoomMember(it) }
                    val text = user?.let {
                        context.getString(R.string.pill_message_from_user, user.displayName)
                    } ?: context.getString(R.string.pill_message_from_unknown_user)
                    MatrixItem.RoomItem(matrixId, text, event?.senderInfo?.avatarUrl)
                } else {
                    val room: RoomSummary? = session?.getRoomSummary(matrixId)
                    val text = room?.displayName?.let {
                        context.getString(R.string.pill_message_in_room, it)
                    } ?: context.getString(R.string.pill_message_in_unknown_room)
                    MatrixItem.RoomItem(matrixId, text, room?.avatarUrl)
                }
            }
            MatrixPatterns.isRoomAlias(matrixId) || MatrixPatterns.isRoomId(matrixId) -> {
                val room: RoomSummary? = session?.getRoomSummary(matrixId)
                if (room == null) {
                    MatrixItem.RoomItem(matrixId, context.getString(R.string.pill_message_unknown_room_or_space))
                } else if (room.roomType == RoomType.SPACE) {
                    MatrixItem.SpaceItem(matrixId, room.displayName, room.avatarUrl)
                } else {
                    MatrixItem.RoomItem(matrixId, room.displayName, room.avatarUrl)
                }
            }
            else -> null
        }
    }

    private fun createPillImageSpan(matrixItem: MatrixItem) =
            PillImageSpan(GlideApp.with(context), avatarRenderer, context, matrixItem)

    private fun addPillSpan(
            renderedText: Spannable,
            pillSpan: PillImageSpan,
            startSpan: Int,
            endSpan: Int
    ) {
        renderedText.setSpan(pillSpan, startSpan, endSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
