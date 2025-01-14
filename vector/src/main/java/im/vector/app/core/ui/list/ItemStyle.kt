/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.ui.list

import android.graphics.Typeface

enum class ItemStyle {
    BIG_TEXT,
    NORMAL_TEXT,
    TITLE,
    SUBHEADER;

    fun toTypeFace(): Typeface {
        return if (this == TITLE) {
            Typeface.DEFAULT_BOLD
        } else {
            Typeface.DEFAULT
        }
    }

    fun toTextSize(): Float {
        return when (this) {
            BIG_TEXT -> 18f
            NORMAL_TEXT -> 14f
            TITLE -> 20f
            SUBHEADER -> 16f
        }
    }
}
