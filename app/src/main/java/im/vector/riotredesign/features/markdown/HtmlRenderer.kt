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

package im.vector.riotredesign.features.markdown

import android.content.Context
import ru.noties.markwon.AbstractMarkwonPlugin
import ru.noties.markwon.Markwon
import ru.noties.markwon.MarkwonVisitor
import ru.noties.markwon.html.HtmlPlugin
import ru.noties.markwon.html.HtmlTag
import ru.noties.markwon.html.MarkwonHtmlRenderer
import ru.noties.markwon.html.TagHandler
import timber.log.Timber

class HtmlRenderer(private val context: Context) {

    private val markwon = Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(MatrixPlugin.create())
            .build()

    fun render(text: String): CharSequence {
        return markwon.toMarkdown(text)
    }

}

private class MatrixPlugin private constructor() : AbstractMarkwonPlugin() {

    override fun configureHtmlRenderer(builder: MarkwonHtmlRenderer.Builder) {
        builder.addHandler("mx-reply", MxReplyTagHandler())
    }

    companion object {

        fun create(): MatrixPlugin {
            return MatrixPlugin()
        }
    }
}

private class MxReplyTagHandler : TagHandler() {
    override fun handle(visitor: MarkwonVisitor, renderer: MarkwonHtmlRenderer, tag: HtmlTag) {
        Timber.v("Handle mx-reply")
    }

}