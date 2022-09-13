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
