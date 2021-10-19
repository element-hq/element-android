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

package org.matrix.android.sdk.api.session.file

import android.net.Uri
import org.matrix.android.sdk.api.session.room.model.message.MessageWithAttachmentContent
import org.matrix.android.sdk.api.session.room.model.message.getFileName
import org.matrix.android.sdk.api.session.room.model.message.getFileUrl
import org.matrix.android.sdk.internal.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.internal.crypto.attachments.toElementToDecrypt
import java.io.File

/**
 * This interface defines methods to get files.
 */
interface FileService {

    sealed class FileState {
        /**
         * The original file is in cache, but the decrypted files can be deleted for security reason.
         * To decrypt the file again, call [downloadFile], the encrypted file will not be downloaded again
         * @param decryptedFileInCache true if the decrypted file is available. Always true for clear files.
         */
        data class InCache(val decryptedFileInCache: Boolean) : FileState()
        object Downloading : FileState()
        object Unknown : FileState()
    }

    /**
     * Download a file if necessary and ensure that if the file is encrypted, the file is decrypted.
     * Result will be a decrypted file, stored in the cache folder. url parameter will be used to create unique filename to avoid name collision.
     */
    suspend fun downloadFile(fileName: String,
                     mimeType: String?,
                     url: String?,
                     elementToDecrypt: ElementToDecrypt?): File

    suspend fun downloadFile(messageContent: MessageWithAttachmentContent): File =
            downloadFile(
                    fileName = messageContent.getFileName(),
                    mimeType = messageContent.mimeType,
                    url = messageContent.getFileUrl(),
                    elementToDecrypt = messageContent.encryptedFileInfo?.toElementToDecrypt()
            )

    fun isFileInCache(mxcUrl: String?,
                      fileName: String,
                      mimeType: String?,
                      elementToDecrypt: ElementToDecrypt?
    ): Boolean

    fun isFileInCache(messageContent: MessageWithAttachmentContent) =
            isFileInCache(
                    mxcUrl = messageContent.getFileUrl(),
                    fileName = messageContent.getFileName(),
                    mimeType = messageContent.mimeType,
                    elementToDecrypt = messageContent.encryptedFileInfo?.toElementToDecrypt())

    /**
     * Use this URI and pass it to intent using flag Intent.FLAG_GRANT_READ_URI_PERMISSION
     * (if not other app won't be able to access it)
     */
    fun getTemporarySharableURI(mxcUrl: String?,
                                fileName: String,
                                mimeType: String?,
                                elementToDecrypt: ElementToDecrypt?): Uri?

    fun getTemporarySharableURI(messageContent: MessageWithAttachmentContent): Uri? =
            getTemporarySharableURI(
                    mxcUrl = messageContent.getFileUrl(),
                    fileName = messageContent.getFileName(),
                    mimeType = messageContent.mimeType,
                    elementToDecrypt = messageContent.encryptedFileInfo?.toElementToDecrypt()
            )

    /**
     * Get information on the given file.
     * Mimetype should be the same one as passed to downloadFile (limitation for now)
     */
    fun fileState(mxcUrl: String?,
                  fileName: String,
                  mimeType: String?,
                  elementToDecrypt: ElementToDecrypt?): FileState

    fun fileState(messageContent: MessageWithAttachmentContent): FileState =
            fileState(
                    mxcUrl = messageContent.getFileUrl(),
                    fileName = messageContent.getFileName(),
                    mimeType = messageContent.mimeType,
                    elementToDecrypt = messageContent.encryptedFileInfo?.toElementToDecrypt()
            )

    /**
     * Clears all the files downloaded by the service, including decrypted files
     */
    fun clearCache()

    /**
     * Clears all the decrypted files by the service
     */
    fun clearDecryptedCache()

    /**
     * Get size of cached files
     */
    fun getCacheSize(): Int
}
