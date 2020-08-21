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

package im.vector.app.features.html

import android.content.Context
import android.text.style.URLSpan
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.glide.GlideRequests
import im.vector.app.features.home.AvatarRenderer
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.SpannableBuilder
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.MarkwonHtmlRenderer
import io.noties.markwon.html.tag.LinkHandler
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.MatrixItem

class MxLinkTagHandler(private val glideRequests: GlideRequests,
                       private val context: Context,
                       private val avatarRenderer: AvatarRenderer,
                       private val sessionHolder: ActiveSessionHolder) : LinkHandler() {

    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        val link = tag.attributes()["href"]
        if (link != null) {
            val permalinkData = PermalinkParser.parse(link)
            val matrixItem = when (permalinkData) {
                is PermalinkData.UserLink  -> {
                    val user = sessionHolder.getSafeActiveSession()?.getUser(permalinkData.userId)
                    MatrixItem.UserItem(permalinkData.userId, user?.displayName, user?.avatarUrl)
                }
                is PermalinkData.RoomLink  -> {
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
            }

            if (matrixItem == null) {
                super.handle(visitor, renderer, tag)
            } else {
                val span = PillImageSpan(glideRequests, avatarRenderer, context, matrixItem)
                SpannableBuilder.setSpans(
                        visitor.builder(),
                        span,
                        tag.start(),
                        tag.end()
                )
                SpannableBuilder.setSpans(
                        visitor.builder(),
                        URLSpan(link),
                        tag.start(),
                        tag.end()
                )
            }
        } else {
            super.handle(visitor, renderer, tag)
        }
    }
}
