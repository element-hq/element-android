/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.intent

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import im.vector.app.core.utils.getFileExtension
import timber.log.Timber
import java.util.Locale

/**
 * Returns the mimetype from a uri.
 *
 * @param context the context
 * @param uri the uri
 * @return the mimetype
 */
fun getMimeTypeFromUri(context: Context, uri: Uri): String? {
    var mimeType: String? = null

    try {
        mimeType = context.contentResolver.getType(uri)

        // try to find the mimetype from the filename
        if (null == mimeType) {
            val extension = getFileExtension(uri.toString())
            if (extension != null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            }
        }

        if (null != mimeType) {
            // the mimetype is sometimes in uppercase.
            mimeType = mimeType.lowercase(Locale.ROOT)
        }
    } catch (e: Exception) {
        Timber.e(e, "Failed to open resource input stream")
    }

    return mimeType
}
