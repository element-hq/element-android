/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.resources

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import im.vector.app.core.utils.getFileExtension
import org.matrix.android.sdk.api.util.MimeTypes
import org.matrix.android.sdk.api.util.MimeTypes.normalizeMimeType
import timber.log.Timber
import java.io.InputStream

data class Resource(
        var mContentStream: InputStream? = null,
        var mMimeType: String? = null
) {
    /**
     * Close the content stream.
     */
    fun close() {
        try {
            mMimeType = null

            mContentStream?.close()
            mContentStream = null
        } catch (e: Exception) {
            Timber.e(e, "Resource.close failed")
        }
    }

    /**
     * Tells if the opened resource is a jpeg one.
     *
     * @return true if the opened resource is a jpeg one.
     */
    fun isJpegResource(): Boolean {
        return mMimeType.normalizeMimeType() == MimeTypes.Jpeg
    }
}

/**
 * Get a resource stream and metadata about it given its URI returned from onActivityResult.
 *
 * @param context the context.
 * @param uri the URI
 * @param providedMimetype the mimetype
 * @return a [Resource] encapsulating the opened resource stream and associated metadata
 * or `null` if opening the resource stream failed.
 */
fun openResource(context: Context, uri: Uri, providedMimetype: String?): Resource? {
    var mimetype = providedMimetype
    try {
        // if the mime type is not provided, try to find it out
        if (mimetype.isNullOrEmpty()) {
            mimetype = context.contentResolver.getType(uri)

            // try to find the mimetype from the filename
            if (null == mimetype) {
                val extension = getFileExtension(uri.toString())
                if (extension != null) {
                    mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                }
            }
        }

        return Resource(context.contentResolver.openInputStream(uri), mimetype)
    } catch (e: Exception) {
        Timber.e(e, "Failed to open resource input stream")
    }

    return null
}
