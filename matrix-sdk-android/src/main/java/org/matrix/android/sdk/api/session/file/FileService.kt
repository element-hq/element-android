/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.file

import android.net.Uri
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.crypto.attachments.ElementToDecrypt
import java.io.File

/**
 * This interface defines methods to get files.
 */
interface FileService {

    enum class DownloadMode {
        /**
         * Download file in external storage
         */
        TO_EXPORT,

        /**
         * Download file in cache
         */
        FOR_INTERNAL_USE,

        /**
         * Download file in file provider path
         */
        FOR_EXTERNAL_SHARE
    }

    enum class FileState {
        IN_CACHE,
        DOWNLOADING,
        UNKNOWN
    }

    /**
     * Download a file.
     * Result will be a decrypted file, stored in the cache folder. url parameter will be used to create unique filename to avoid name collision.
     */
    fun downloadFile(
            downloadMode: DownloadMode,
            id: String,
            fileName: String,
            mimeType: String?,
            url: String?,
            elementToDecrypt: ElementToDecrypt?,
            callback: MatrixCallback<File>): Cancelable

    fun isFileInCache(mxcUrl: String, mimeType: String?): Boolean

    /**
     * Use this URI and pass it to intent using flag Intent.FLAG_GRANT_READ_URI_PERMISSION
     * (if not other app won't be able to access it)
     */
    fun getTemporarySharableURI(mxcUrl: String, mimeType: String?): Uri?

    /**
     * Get information on the given file.
     * Mimetype should be the same one as passed to downloadFile (limitation for now)
     */
    fun fileState(mxcUrl: String, mimeType: String?): FileState

    /**
     * Clears all the files downloaded by the service
     */
    fun clearCache()

    /**
     * Get size of cached files
     */
    fun getCacheSize(): Int
}
