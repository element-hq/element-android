/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.html

import android.content.Context
import android.text.Spannable
import android.text.Spanned
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.glide.GlideApp
import im.vector.app.features.home.AvatarRenderer
import io.noties.markwon.core.spans.LinkSpan
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem

class PillsPostProcessor @AssistedInject constructor(@Assisted private val roomId: String?,
                                                     private val context: Context,
                                                     private val avatarRenderer: AvatarRenderer,
                                                     private val sessionHolder: ActiveSessionHolder)
    : EventHtmlRenderer.PostProcessor {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String?): PillsPostProcessor
    }

    override fun afterRender(renderedText: Spannable) {
        addPillSpans(renderedText, roomId)
    }

    private fun addPillSpans(renderedText: Spannable, roomId: String?) {
        // We let markdown handle links and then we add PillImageSpan if needed.
        val linkSpans = renderedText.getSpans(0, renderedText.length, LinkSpan::class.java)
        linkSpans.forEach { linkSpan ->
            val pillSpan = linkSpan.createPillSpan(roomId) ?: return@forEach
            val startSpan = renderedText.getSpanStart(linkSpan)
            val endSpan = renderedText.getSpanEnd(linkSpan)
            renderedText.setSpan(pillSpan, startSpan, endSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun LinkSpan.createPillSpan(roomId: String?): PillImageSpan? {
        val permalinkData = PermalinkParser.parse(url)
        val matrixItem = when (permalinkData) {
            is PermalinkData.UserLink -> {
                if (roomId == null) {
                    sessionHolder.getSafeActiveSession()?.getUser(permalinkData.userId)?.toMatrixItem()
                } else {
                    sessionHolder.getSafeActiveSession()?.getRoomMember(permalinkData.userId, roomId)?.toMatrixItem()
                }
            }
            is PermalinkData.RoomLink -> {
                if (permalinkData.eventId == null) {
                    val room: RoomSummary? = sessionHolder.getSafeActiveSession()?.getRoomSummary(permalinkData.roomIdOrAlias)
                    if (permalinkData.isRoomAlias) {
                        MatrixItem.RoomAliasItem(permalinkData.roomIdOrAlias, room?.displayName, room?.avatarUrl)
                    } else {
                        MatrixItem.RoomItem(permalinkData.roomIdOrAlias, room?.displayName, room?.avatarUrl)
                    }
                } else {
                    // Exclude event link (used in reply events, we do not want to pill the "in reply to")
                    null
                }
            }
            is PermalinkData.GroupLink -> {
                val group = sessionHolder.getSafeActiveSession()?.getGroupSummary(permalinkData.groupId)
                MatrixItem.GroupItem(permalinkData.groupId, group?.displayName, group?.avatarUrl)
            }
            else                       -> null
        } ?: return null
        return PillImageSpan(GlideApp.with(context), avatarRenderer, context, matrixItem)
    }
}
