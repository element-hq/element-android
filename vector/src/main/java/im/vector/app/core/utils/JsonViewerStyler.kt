/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import im.vector.app.core.resources.ColorProvider
import org.billcarsonfr.jsonviewer.JSonViewerStyleProvider

fun createJSonViewerStyleProvider(colorProvider: ColorProvider): JSonViewerStyleProvider {
    return JSonViewerStyleProvider(
            keyColor = colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary),
            secondaryColor = colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_secondary),
            stringColor = colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_notice_text_color),
            baseColor = colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_primary),
            booleanColor = colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_notice_text_color),
            numberColor = colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_notice_text_color)
    )
}
