/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.commonmark.ext.maths

import org.commonmark.Extension
import org.commonmark.ext.maths.internal.DollarMathsDelimiterProcessor
import org.commonmark.ext.maths.internal.MathsHtmlNodeRenderer
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

internal class MathsExtension private constructor() : Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {
    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.customDelimiterProcessor(DollarMathsDelimiterProcessor())
    }

    override fun extend(rendererBuilder: HtmlRenderer.Builder) {
        rendererBuilder.nodeRendererFactory { context -> MathsHtmlNodeRenderer(context) }
    }

    companion object {
        fun create(): Extension {
            return MathsExtension()
        }
    }
}
