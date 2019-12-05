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

package im.vector.riotx.features.html

import android.content.Context
import android.text.style.URLSpan
import im.vector.matrix.android.api.permalinks.PermalinkData
import im.vector.matrix.android.api.permalinks.PermalinkParser
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.glide.GlideRequests
import im.vector.riotx.features.home.AvatarRenderer
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.SpannableBuilder
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.MarkwonHtmlRenderer
import io.noties.markwon.html.tag.LinkHandler

class MxLinkTagHandler(private val glideRequests: GlideRequests,
                       private val context: Context,
                       private val avatarRenderer: AvatarRenderer,
                       private val sessionHolder: ActiveSessionHolder) : LinkHandler() {

    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        val link = tag.attributes()["href"]
        if (link != null) {
            val permalinkData = PermalinkParser.parse(link)
            when (permalinkData) {
                is PermalinkData.UserLink -> {
                    val user = sessionHolder.getSafeActiveSession()?.getUser(permalinkData.userId)
                    val span = PillImageSpan(glideRequests, avatarRenderer, context, permalinkData.userId, user?.displayName
                            ?: permalinkData.userId, user?.avatarUrl)
                    SpannableBuilder.setSpans(
                            visitor.builder(),
                            span,
                            tag.start(),
                            tag.end()
                    )
                    // also add clickable span
                    SpannableBuilder.setSpans(
                            visitor.builder(),
                            URLSpan(link),
                            tag.start(),
                            tag.end()
                    )
                }
                else                      -> super.handle(visitor, renderer, tag)
            }
        } else {
            super.handle(visitor, renderer, tag)
        }
    }
}
