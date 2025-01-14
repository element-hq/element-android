/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import android.view.MenuItem
import androidx.annotation.ColorInt
import androidx.core.text.toSpannable
import im.vector.app.core.utils.colorizeMatchingText

fun MenuItem.setTextColor(@ColorInt color: Int) {
    val currentTitle = title.orEmpty().toString()
    title = currentTitle
            .toSpannable()
            .colorizeMatchingText(currentTitle, color)
}
