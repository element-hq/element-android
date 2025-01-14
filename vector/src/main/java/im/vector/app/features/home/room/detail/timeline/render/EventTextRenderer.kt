/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.glide.GlideApp
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.html.PillImageSpan
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.getUserOrDefault
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem

class EventTextRenderer @AssistedInject constructor(
        @Assisted private val roomId: String?,
        private val context: Context,
        private val avatarRenderer: AvatarRenderer,
        private val sessionHolder: ActiveSessionHolder,
) {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String?): EventTextRenderer
    }

    /**
     * @param text the text to be rendered
     */
    fun render(text: CharSequence): CharSequence {
        val formattedText = renderPermalinks(text)
        return renderNotifyEveryone(formattedText)
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
        val room: RoomSummary? = sessionHolder.getSafeActiveSession()?.roomService()?.getRoomSummary(roomId)
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
        for (match in Patterns.WEB_URL.toRegex().findAll(text)) {
            val url = text.substring(match.range)
            val supportedHosts = context.resources.getStringArray(im.vector.app.config.R.array.permalink_supported_hosts)
            val isPermalinkSupported = sessionHolder.getSafeActiveSession()?.permalinkService()?.isPermalinkSupported(supportedHosts, url).orFalse()
            val matrixItem = if (isPermalinkSupported) {
                when (val permalinkData = PermalinkParser.parse(url)) {
                    is PermalinkData.UserLink -> permalinkData.toMatrixItem()
                    is PermalinkData.RoomLink -> permalinkData.toMatrixItem()
                    else -> null
                }
            } else null

            if (matrixItem != null) {
                addPillSpan(text, createPillImageSpan(matrixItem), match.range.first, match.range.last + 1)
            }
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

    private fun PermalinkData.UserLink.toMatrixItem(): MatrixItem? =
            roomId?.let { sessionHolder.getSafeActiveSession()?.roomService()?.getRoomMember(userId, it)?.toMatrixItem() }
                    ?: sessionHolder.getSafeActiveSession()?.getUserOrDefault(userId)?.toMatrixItem()

    private fun PermalinkData.RoomLink.toMatrixItem(): MatrixItem =
            if (eventId.isNullOrEmpty()) {
                val room: RoomSummary? = sessionHolder.getSafeActiveSession()?.getRoomSummary(roomIdOrAlias)
                when {
                    isRoomAlias -> MatrixItem.RoomAliasItem(roomIdOrAlias, room?.displayName, room?.avatarUrl)
                    room == null -> MatrixItem.RoomItem(roomIdOrAlias, context.getString(CommonStrings.pill_message_unknown_room_or_space))
                    room.roomType == RoomType.SPACE -> MatrixItem.SpaceItem(roomIdOrAlias, room.displayName, room.avatarUrl)
                    else -> MatrixItem.RoomItem(roomIdOrAlias, room.displayName, room.avatarUrl)
                }
            } else {
                if (roomIdOrAlias == roomId) {
                    val session = sessionHolder.getSafeActiveSession()
                    val event = session?.eventService()?.getEventFromCache(roomId, eventId!!)
                    val user = event?.senderId?.let { session.roomService().getRoomMember(it, roomId) }
                    val text = user?.let {
                        context.getString(CommonStrings.pill_message_from_user, user.displayName)
                    } ?: context.getString(CommonStrings.pill_message_from_unknown_user)
                    MatrixItem.RoomItem(roomIdOrAlias, text, user?.avatarUrl, user?.displayName)
                } else {
                    val room: RoomSummary? = sessionHolder.getSafeActiveSession()?.getRoomSummary(roomIdOrAlias)
                    when {
                        isRoomAlias -> MatrixItem.RoomAliasItem(
                                roomIdOrAlias,
                                context.getString(CommonStrings.pill_message_in_room, room?.displayName ?: roomIdOrAlias),
                                room?.avatarUrl,
                                room?.displayName
                        )
                        room != null -> MatrixItem.RoomItem(
                                roomIdOrAlias,
                                context.getString(CommonStrings.pill_message_in_room, room.displayName),
                                room.avatarUrl,
                                room.displayName
                        )
                        else -> MatrixItem.RoomItem(roomIdOrAlias, context.getString(CommonStrings.pill_message_in_unknown_room))
                    }
                }
            }
}
