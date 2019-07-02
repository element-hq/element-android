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
import android.text.style.URLSpan
import androidx.appcompat.app.AppCompatActivity
import im.vector.matrix.android.api.permalinks.PermalinkData
import im.vector.matrix.android.api.permalinks.PermalinkParser
import im.vector.matrix.android.api.session.Session
import im.vector.riotx.core.glide.GlideApp
import im.vector.riotx.core.glide.GlideRequests
import im.vector.riotx.features.home.AvatarRenderer
import org.commonmark.node.BlockQuote
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Node
import ru.noties.markwon.*
import ru.noties.markwon.html.HtmlTag
import ru.noties.markwon.html.MarkwonHtmlParserImpl
import ru.noties.markwon.html.MarkwonHtmlRenderer
import ru.noties.markwon.html.TagHandler
import ru.noties.markwon.html.tag.*
import java.util.Arrays.asList
import javax.inject.Inject

class EventHtmlRenderer @Inject constructor(context: AppCompatActivity,
                                            val avatarRenderer: AvatarRenderer,
                                            session: Session) {
    private val markwon = Markwon.builder(context)
            .usePlugin(MatrixPlugin.create(GlideApp.with(context), context, avatarRenderer, session))
            .build()

    fun render(text: String): CharSequence {
        return markwon.toMarkdown(text)
    }

}

private class MatrixPlugin private constructor(private val glideRequests: GlideRequests,
                                               private val context: Context,
                                               private val avatarRenderer: AvatarRenderer,
                                               private val session: Session) : AbstractMarkwonPlugin() {

    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
        builder.htmlParser(MarkwonHtmlParserImpl.create())
    }

    override fun configureHtmlRenderer(builder: MarkwonHtmlRenderer.Builder) {
        builder
                .setHandler(
                        "img",
                        ImageHandler.create())
                .setHandler(
                        "a",
                        MxLinkHandler(glideRequests, context, avatarRenderer, session))
                .setHandler(
                        "blockquote",
                        BlockquoteHandler())
                .setHandler(
                        "font",
                        FontTagHandler())
                .setHandler(
                        "sub",
                        SubScriptHandler())
                .setHandler(
                        "sup",
                        SuperScriptHandler())
                .setHandler(
                        asList<String>("b", "strong"),
                        StrongEmphasisHandler())
                .setHandler(
                        asList<String>("s", "del"),
                        StrikeHandler())
                .setHandler(
                        asList<String>("u", "ins"),
                        UnderlineHandler())
                .setHandler(
                        asList<String>("ul", "ol"),
                        ListHandler())
                .setHandler(
                        asList<String>("i", "em", "cite", "dfn"),
                        EmphasisHandler())
                .setHandler(
                        asList<String>("h1", "h2", "h3", "h4", "h5", "h6"),
                        HeadingHandler())
                .setHandler("mx-reply",
                        MxReplyTagHandler())

    }

    override fun afterRender(node: Node, visitor: MarkwonVisitor) {
        val configuration = visitor.configuration()
        configuration.htmlRenderer().render(visitor, configuration.htmlParser())
    }

    override fun configureVisitor(builder: MarkwonVisitor.Builder) {
        builder
                .on(HtmlBlock::class.java) { visitor, htmlBlock -> visitHtml(visitor, htmlBlock.literal) }
                .on(HtmlInline::class.java) { visitor, htmlInline -> visitHtml(visitor, htmlInline.literal) }
    }

    private fun visitHtml(visitor: MarkwonVisitor, html: String?) {
        if (html != null) {
            visitor.configuration().htmlParser().processFragment(visitor.builder(), html)
        }
    }

    companion object {

        fun create(glideRequests: GlideRequests, context: Context, avatarRenderer: AvatarRenderer, session: Session): MatrixPlugin {
            return MatrixPlugin(glideRequests, context, avatarRenderer, session)
        }
    }
}

private class MxLinkHandler(private val glideRequests: GlideRequests,
                            private val context: Context,
                            private val avatarRenderer: AvatarRenderer,
                            private val session: Session) : TagHandler() {

    private val linkHandler = LinkHandler()

    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        val link = tag.attributes()["href"]
        if (link != null) {
            val permalinkData = PermalinkParser.parse(link)
            when (permalinkData) {
                is PermalinkData.UserLink -> {
                    val user = session.getUser(permalinkData.userId)
                    val span = PillImageSpan(glideRequests, avatarRenderer, context, permalinkData.userId, user)
                    SpannableBuilder.setSpans(
                            visitor.builder(),
                            span,
                            tag.start(),
                            tag.end()
                    )
                    //also add clickable span
                    SpannableBuilder.setSpans(
                            visitor.builder(),
                            URLSpan(link),
                            tag.start(),
                            tag.end()
                    )
                }
                else                      -> linkHandler.handle(visitor, renderer, tag)
            }
        } else {
            linkHandler.handle(visitor, renderer, tag)
        }
    }

}

private class MxReplyTagHandler : TagHandler() {

    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        val configuration = visitor.configuration()
        val factory = configuration.spansFactory().get(BlockQuote::class.java)
        if (factory != null) {
            SpannableBuilder.setSpans(
                    visitor.builder(),
                    factory.getSpans(configuration, visitor.renderProps()),
                    tag.start(),
                    tag.end()
            )
            val replyText = visitor.builder().removeFromEnd(tag.end())
            visitor.builder().append("\n\n").append(replyText)
        }
    }

}
