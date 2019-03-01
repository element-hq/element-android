/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.riotredesign.features.html

import android.content.Context
import im.vector.matrix.android.api.permalinks.PermalinkData
import im.vector.matrix.android.api.permalinks.PermalinkParser
import im.vector.matrix.android.api.session.Session
import im.vector.riotredesign.core.glide.GlideRequests
import org.commonmark.node.BlockQuote
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Node
import ru.noties.markwon.AbstractMarkwonPlugin
import ru.noties.markwon.Markwon
import ru.noties.markwon.MarkwonConfiguration
import ru.noties.markwon.MarkwonVisitor
import ru.noties.markwon.SpannableBuilder
import ru.noties.markwon.html.HtmlTag
import ru.noties.markwon.html.MarkwonHtmlParserImpl
import ru.noties.markwon.html.MarkwonHtmlRenderer
import ru.noties.markwon.html.TagHandler
import ru.noties.markwon.html.tag.BlockquoteHandler
import ru.noties.markwon.html.tag.EmphasisHandler
import ru.noties.markwon.html.tag.HeadingHandler
import ru.noties.markwon.html.tag.ImageHandler
import ru.noties.markwon.html.tag.LinkHandler
import ru.noties.markwon.html.tag.ListHandler
import ru.noties.markwon.html.tag.StrikeHandler
import ru.noties.markwon.html.tag.StrongEmphasisHandler
import ru.noties.markwon.html.tag.SubScriptHandler
import ru.noties.markwon.html.tag.SuperScriptHandler
import ru.noties.markwon.html.tag.UnderlineHandler
import java.util.Arrays.asList

class EventHtmlRenderer(glideRequests: GlideRequests,
                        context: Context,
                        session: Session) {

    private val markwon = Markwon.builder(context)
            .usePlugin(MatrixPlugin.create(glideRequests, context, session))
            .build()

    fun render(text: String): CharSequence {
        return markwon.toMarkdown(text)
    }

}

private class MatrixPlugin private constructor(private val glideRequests: GlideRequests,
                                               private val context: Context,
                                               private val session: Session) : AbstractMarkwonPlugin() {

    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
        builder.htmlParser(MarkwonHtmlParserImpl.create())
    }

    override fun configureHtmlRenderer(builder: MarkwonHtmlRenderer.Builder) {
        builder
                .addHandler(
                        "img",
                        ImageHandler.create())
                .addHandler(
                        "a",
                        MxLinkHandler(glideRequests, context, session))
                .addHandler(
                        "blockquote",
                        BlockquoteHandler())
                .addHandler(
                        "sub",
                        SubScriptHandler())
                .addHandler(
                        "sup",
                        SuperScriptHandler())
                .addHandler(
                        asList<String>("b", "strong"),
                        StrongEmphasisHandler())
                .addHandler(
                        asList<String>("s", "del"),
                        StrikeHandler())
                .addHandler(
                        asList<String>("u", "ins"),
                        UnderlineHandler())
                .addHandler(
                        asList<String>("ul", "ol"),
                        ListHandler())
                .addHandler(
                        asList<String>("i", "em", "cite", "dfn"),
                        EmphasisHandler())
                .addHandler(
                        asList<String>("h1", "h2", "h3", "h4", "h5", "h6"),
                        HeadingHandler())
                .addHandler("mx-reply",
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

        fun create(glideRequests: GlideRequests, context: Context, session: Session): MatrixPlugin {
            return MatrixPlugin(glideRequests, context, session)
        }
    }
}

private class MxLinkHandler(private val glideRequests: GlideRequests,
                            private val context: Context,
                            private val session: Session) : TagHandler() {

    private val linkHandler = LinkHandler()

    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        val link = tag.attributes()["href"]
        if (link != null) {
            val permalinkData = PermalinkParser.parse(link)
            when (permalinkData) {
                is PermalinkData.UserLink -> {
                    val user = session.getUser(permalinkData.userId) ?: return
                    val span = PillImageSpan(glideRequests, context, permalinkData.userId, user)
                    SpannableBuilder.setSpans(
                            visitor.builder(),
                            span,
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
