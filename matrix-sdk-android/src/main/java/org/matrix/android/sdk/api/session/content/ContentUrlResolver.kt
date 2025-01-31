/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.content

import org.matrix.android.sdk.api.session.crypto.attachments.ElementToDecrypt

/**
 * This interface defines methods for accessing content from the current session.
 */
interface ContentUrlResolver {

    enum class ThumbnailMethod(val value: String) {
        CROP("crop"),
        SCALE("scale")
    }

    /**
     * URL to use to upload content.
     */
    val uploadUrl: String

    /**
     * Get the actual URL for accessing the full-size image of a Matrix media content URI.
     *
     * @param contentUrl the Matrix media content URI (in the form of "mxc://...").
     * @return the URL to access the described resource, or null if the url is invalid.
     */
    fun resolveFullSize(contentUrl: String?): String?

    /**
     * Get the ResolvedMethod to download a URL.
     *
     * @param contentUrl the Matrix media content URI (in the form of "mxc://...").
     * @param elementToDecrypt Encryption data may be required if you use a content scanner
     * @return the Method to access resource, or null if invalid
     */
    fun resolveForDownload(contentUrl: String?, elementToDecrypt: ElementToDecrypt? = null): ResolvedMethod?

    /**
     * Get the actual URL for accessing the thumbnail image of a given Matrix media content URI.
     *
     * @param contentUrl the Matrix media content URI (in the form of "mxc://...").
     * @param width the desired width
     * @param height the desired height
     * @param method the desired method (METHOD_CROP or METHOD_SCALE)
     * @return the URL to access the described resource, or null if the url is invalid.
     */
    fun resolveThumbnail(contentUrl: String?, width: Int, height: Int, method: ThumbnailMethod): String?

    sealed class ResolvedMethod {
        data class GET(val url: String) : ResolvedMethod()
        data class POST(val url: String, val jsonBody: String) : ResolvedMethod()
    }
}
