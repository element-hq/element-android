/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker.utils

import android.database.Cursor
import androidx.core.database.getStringOrNull

fun Cursor.getColumnIndexOrNull(column: String): Int? {
    return getColumnIndex(column).takeIf { it != -1 }
}

fun Cursor.readStringColumnOrNull(column: String): String? {
    return getColumnIndexOrNull(column)?.let { getStringOrNull(it) }
}
