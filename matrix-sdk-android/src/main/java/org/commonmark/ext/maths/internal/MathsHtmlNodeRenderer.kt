/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
        val attributes = context.extendAttributes(
                node, if (display) "div" else "span", Collections.singletonMap(
                "data-mx-maths",
                latex
        )
        )
        html.tag(if (display) "div" else "span", attributes)
        html.tag("code")
        context.render(contents)
        html.tag("/code")
        html.tag(if (display) "/div" else "/span")
    }
}
