/*
 * Copyright (c) 2024 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.util.file

import android.webkit.MimeTypeMap
import org.matrix.android.sdk.internal.session.DefaultFileService.Companion.DEFAULT_FILENAME

/**
 * Remove any characters from the file name that are not supported by the Android OS,
 * and update the file extension to match the mimeType.
 */
fun safeFileName(fileName: String?, mimeType: String?): String {
    return buildString {
        // filename has to be safe for the Android System
        val result = fileName
                ?.replace("[\\\\?%*:|\"<>\\s]".toRegex(), "_")
                ?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_FILENAME
        append(result)
        // Check that the extension is correct regarding the mimeType
        val extensionFromMime = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) }
        if (extensionFromMime != null) {
            // Compare
            val fileExtension = result.substringAfterLast(delimiter = ".", missingDelimiterValue = "")
            if (fileExtension.isEmpty() || fileExtension != extensionFromMime) {
                // Missing extension, or diff in extension, add the one provided by the mimetype
                append(".")
                append(extensionFromMime)
            }
        }
    }
}
