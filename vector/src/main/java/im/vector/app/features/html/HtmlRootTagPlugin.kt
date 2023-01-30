/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.html

import io.noties.markwon.AbstractMarkwonPlugin

/**
 * A root node enables post-processing of optionally nested tags.
 * See: [im.vector.app.features.html.CodePostProcessorTagHandler]
 */
internal class HtmlRootTagPlugin : AbstractMarkwonPlugin() {
    companion object {
        const val ROOT_ATTRIBUTE = "data-root"
        const val ROOT_TAG_NAME = "div"
    }
    override fun processMarkdown(html: String): String {
        return "<$ROOT_TAG_NAME $ROOT_ATTRIBUTE>$html</$ROOT_TAG_NAME>"
    }
}
