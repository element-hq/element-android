/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.render

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.glide.GlideApp
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.html.PillImageSpan
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.MatrixItem

class EventTextRenderer @AssistedInject constructor(
        @Assisted private val roomId: String?,
        private val context: Context,
        private val avatarRenderer: AvatarRenderer,
        private val sessionHolder: ActiveSessionHolder
) {

    /* ==========================================================================================
     * Public api
     * ========================================================================================== */

    @AssistedFactory
    interface Factory {
        fun create(roomId: String?): EventTextRenderer
    }

    /**
     * @param text the text you want to render
     */
    fun render(text: CharSequence): CharSequence {
        return if (roomId != null && text.contains(MatrixItem.NOTIFY_EVERYONE)) {
            SpannableStringBuilder(text).apply {
                addNotifyEveryoneSpans(this, roomId)
            }
        } else {
            text
        }
    }

    /* ==========================================================================================
     * Helper methods
     * ========================================================================================== */

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
