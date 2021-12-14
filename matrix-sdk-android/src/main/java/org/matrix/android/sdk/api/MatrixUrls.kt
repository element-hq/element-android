/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api

/**
 * This class contains pattern to match Matrix Url, aka mxc urls
 */
object MatrixUrls {
    /**
     * "mxc" scheme, including "://". So "mxc://"
     */
    const val MATRIX_CONTENT_URI_SCHEME = "mxc://"

    /**
     * Return true if the String starts with "mxc://"
     */
    fun String.isMxcUrl() = startsWith(MATRIX_CONTENT_URI_SCHEME)

    /**
     * Remove the "mxc://" prefix. No op if the String is not a Mxc URL
     */
    fun String.removeMxcPrefix() = removePrefix(MATRIX_CONTENT_URI_SCHEME)
}
