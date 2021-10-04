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

package org.matrix.android.sdk.api.session.room.model.message

interface MessageContentWithFormattedBody : MessageContent {
    /**
     * The format used in the formatted_body. Currently only "org.matrix.custom.html" is supported.
     */
    val format: String?

    /**
     * The formatted version of the body. This is required if format is specified.
     */
    val formattedBody: String?

    /**
     * Get the formattedBody, only if not blank and if the format is equal to "org.matrix.custom.html"
     */
    val matrixFormattedBody: String?
        get() = formattedBody?.takeIf { it.isNotBlank() && format == MessageFormat.FORMAT_MATRIX_HTML }
}
