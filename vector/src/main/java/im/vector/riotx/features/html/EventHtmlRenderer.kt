/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotx.features.html

import android.content.Context
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.glide.GlideApp
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.features.home.AvatarRenderer
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.html.TagHandlerNoOp
import org.commonmark.node.Node
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventHtmlRenderer @Inject constructor(context: Context,
                                            htmlConfigure: MatrixHtmlPluginConfigure) {

    private val markwon = Markwon.builder(context)
            .usePlugin(HtmlPlugin.create(htmlConfigure))
            .build()

    fun parse(text: String): Node {
        return markwon.parse(text)
    }

    fun render(text: String): CharSequence {
        return try {
            markwon.toMarkdown(text)
        } catch (failure: Throwable) {
            Timber.v("Fail to render $text to html")
            text
        }
    }

    fun render(node: Node): CharSequence? {
        return try {
            markwon.render(node)
        } catch (failure: Throwable) {
            Timber.v("Fail to render $node to html")
            return null
        }
    }
}

class MatrixHtmlPluginConfigure @Inject constructor(private val context: Context,
                                                    private val colorProvider: ColorProvider,
                                                    private val avatarRenderer: AvatarRenderer,
                                                    private val session: ActiveSessionHolder) : HtmlPlugin.HtmlConfigure {

    override fun configureHtml(plugin: HtmlPlugin) {
        plugin
                .addHandler(TagHandlerNoOp.create("a"))
                .addHandler(FontTagHandler())
                .addHandler(MxLinkTagHandler(GlideApp.with(context), context, avatarRenderer, session))
                .addHandler(MxReplyTagHandler())
                .addHandler(SpanHandler(colorProvider))
    }
}
