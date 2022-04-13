/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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
package org.commonmark.ext.maths.internal

import org.commonmark.ext.maths.DisplayMaths
import org.commonmark.node.Node
import org.commonmark.node.Text
import org.commonmark.renderer.html.HtmlNodeRendererContext
import org.commonmark.renderer.html.HtmlWriter
import java.util.Collections

internal class MathsHtmlNodeRenderer(private val context: HtmlNodeRendererContext) : MathsNodeRenderer() {
    private val html: HtmlWriter = context.writer
    override fun render(node: Node) {
        val display = node.javaClass == DisplayMaths::class.java
        val contents = node.firstChild // should be the only child
        val latex = (contents as Text).literal
        val attributes = context.extendAttributes(node, if (display) "div" else "span", Collections.singletonMap("data-mx-maths",
                latex))
        html.tag(if (display) "div" else "span", attributes)
        html.tag("code")
        context.render(contents)
        html.tag("/code")
        html.tag(if (display) "/div" else "/span")
    }
}
