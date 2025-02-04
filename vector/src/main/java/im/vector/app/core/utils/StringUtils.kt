/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import java.util.Locale

// String.capitalize is now deprecated
fun String.safeCapitalize(locale: Locale): String {
    return replaceFirstChar { char ->
        if (char.isLowerCase()) {
            char.titlecase(locale)
        } else {
            char.toString()
        }
    }
}
