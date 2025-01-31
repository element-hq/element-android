/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.media

/**
 * Facility data class to get the common field of a PreviewUrl response form the server.
 *
 * Example of return data for the url `https://matrix.org`:
 * <pre>
 * {
 *     "matrix:image:size": 112805,
 *     "og:description": "Matrix is an open standard for interoperable, decentralised, real-time communication",
 *     "og:image": "mxc://matrix.org/2020-12-03_uFqjagCCTJbaaJxb",
 *     "og:image:alt": "Matrix is an open standard for interoperable, decentralised, real-time communication",
 *     "og:image:height": 467,
 *     "og:image:type": "image/jpeg",
 *     "og:image:width": 911,
 *     "og:locale": "en_US",
 *     "og:site_name": "Matrix.org",
 *     "og:title": "Matrix.org",
 *     "og:type": "website",
 *     "og:url": "https://matrix.org"
 * }
 * </pre>
 */
data class PreviewUrlData(
        // Value of field "og:url". If not provided, this is the value passed in parameter
        val url: String,
        // Value of field "og:site_name"
        val siteName: String?,
        // Value of field "og:title"
        val title: String?,
        // Value of field "og:description"
        val description: String?,
        // Value of field "og:image"
        val mxcUrl: String?,
        // Value of field "og:image:width"
        val imageWidth: Int?,
        // Value of field "og:image:height"
        val imageHeight: Int?
)
