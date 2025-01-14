/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
