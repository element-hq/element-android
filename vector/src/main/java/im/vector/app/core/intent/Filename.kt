/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.intent

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.database.getStringOrNull
import im.vector.lib.multipicker.utils.getColumnIndexOrNull

fun getFilenameFromUri(context: Context?, uri: Uri): String? {
    if (context != null && uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        return cursor.getColumnIndexOrNull(OpenableColumns.DISPLAY_NAME)
                                ?.let { cursor.getStringOrNull(it) }
                    }
                }
    }
    return uri.path?.substringAfterLast('/')
}
