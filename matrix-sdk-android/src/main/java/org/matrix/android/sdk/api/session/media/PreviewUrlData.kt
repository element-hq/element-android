/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.session.media

/**
 * Facility data class to get the common field of a PreviewUrl response form the server
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
