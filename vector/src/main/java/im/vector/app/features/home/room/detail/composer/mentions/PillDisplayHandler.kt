/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.composer.mentions

import android.text.style.ReplacementSpan
import io.element.android.wysiwyg.display.MentionDisplayHandler
import io.element.android.wysiwyg.display.TextDisplay
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toEveryoneInRoomMatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.api.util.toRoomAliasMatrixItem

/**
 * A rich text editor [MentionDisplayHandler] that helps with replacing user and room links with pills.
 */
internal class PillDisplayHandler(
        private val roomId: String,
        private val getRoom: (roomId: String) -> RoomSummary?,
        private val getMember: (userId: String) -> RoomMemberSummary?,
        private val replacementSpanFactory: (matrixItem: MatrixItem) -> ReplacementSpan,
) : MentionDisplayHandler {
    override fun resolveMentionDisplay(text: String, url: String): TextDisplay {
        val matrixItem = when (val permalink = PermalinkParser.parse(url)) {
            is PermalinkData.UserLink -> {
                val userId = permalink.userId
                when (val roomMember = getMember(userId)) {
                    null -> MatrixItem.UserItem(userId, userId, null)
                    else -> roomMember.toMatrixItem()
                }
            }
            is PermalinkData.RoomLink -> {
                val roomId = permalink.roomIdOrAlias
                val room = getRoom(roomId)
                when {
                    room == null -> MatrixItem.RoomItem(roomId, roomId, null)
                    text == MatrixItem.NOTIFY_EVERYONE -> room.toEveryoneInRoomMatrixItem()
                    permalink.isRoomAlias -> room.toRoomAliasMatrixItem()
                    else -> room.toMatrixItem()
                }
            }
            else ->
                return TextDisplay.Plain
        }
        val replacement = replacementSpanFactory.invoke(matrixItem)
        return TextDisplay.Custom(customSpan = replacement)
    }

    override fun resolveAtRoomMentionDisplay(): TextDisplay {
        val matrixItem = getRoom(roomId)?.toEveryoneInRoomMatrixItem()
                ?: MatrixItem.EveryoneInRoomItem(roomId)
        return TextDisplay.Custom(replacementSpanFactory.invoke(matrixItem))
    }
}
