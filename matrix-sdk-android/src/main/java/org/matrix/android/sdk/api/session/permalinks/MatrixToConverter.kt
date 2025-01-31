/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.permalinks

import android.net.Uri

/**
 * Mapping of an input URI to a matrix.to compliant URI.
 */
object MatrixToConverter {

    /**
     * Try to convert a URL from an element web instance or from a client permalink to a matrix.to url.
     * To be successfully converted, URL path should contain one of the [SUPPORTED_PATHS].
     * Examples:
     * - https://riot.im/develop/#/room/#element-android:matrix.org  ->  https://matrix.to/#/#element-android:matrix.org
     * - https://app.element.io/#/room/#element-android:matrix.org   ->  https://matrix.to/#/#element-android:matrix.org
     * - https://www.example.org/#/room/#element-android:matrix.org  ->  https://matrix.to/#/#element-android:matrix.org
     */
    fun convert(uri: Uri): Uri? {
        val uriString = uri.toString()

        return when {
            // URL is already a matrix.to
            uriString.startsWith(PermalinkService.MATRIX_TO_URL_BASE) -> uri
            // Web or client url
            SUPPORTED_PATHS.any { it in uriString } -> {
                val path = SUPPORTED_PATHS.first { it in uriString }
                Uri.parse(PermalinkService.MATRIX_TO_URL_BASE + uriString.substringAfter(path))
            }
            // URL is not supported
            else -> null
        }
    }

    private val SUPPORTED_PATHS = listOf(
            "/#/room/",
            "/#/user/",
            "/#/group/"
    )
}
