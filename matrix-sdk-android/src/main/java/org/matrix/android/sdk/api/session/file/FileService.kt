/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.file

import android.net.Uri
import org.matrix.android.sdk.api.session.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.api.session.crypto.attachments.toElementToDecrypt
import org.matrix.android.sdk.api.session.room.model.message.MessageWithAttachmentContent
import org.matrix.android.sdk.api.session.room.model.message.getFileName
import org.matrix.android.sdk.api.session.room.model.message.getFileUrl
import java.io.File

/**
 * This interface defines methods to get files.
 */
interface FileService {

    sealed class FileState {
        /**
         * The original file is in cache, but the decrypted files can be deleted for security reason.
         * To decrypt the file again, call [downloadFile], the encrypted file will not be downloaded again
         * @property decryptedFileInCache true if the decrypted file is available. Always true for clear files.
         */
        data class InCache(val decryptedFileInCache: Boolean) : FileState()
        object Downloading : FileState()
        object Unknown : FileState()
    }

    /**
     * Download a file if necessary and ensure that if the file is encrypted, the file is decrypted.
     * Result will be a decrypted file, stored in the cache folder. url parameter will be used to create unique filename to avoid name collision.
     */
    suspend fun downloadFile(
            fileName: String,
            mimeType: String?,
            url: String?,
            elementToDecrypt: ElementToDecrypt?
    ): File

    suspend fun downloadFile(messageContent: MessageWithAttachmentContent): File =
            downloadFile(
                    fileName = messageContent.getFileName(),
                    mimeType = messageContent.mimeType,
                    url = messageContent.getFileUrl(),
                    elementToDecrypt = messageContent.encryptedFileInfo?.toElementToDecrypt()
            )

    fun isFileInCache(
            mxcUrl: String?,
            fileName: String,
            mimeType: String?,
            elementToDecrypt: ElementToDecrypt?
    ): Boolean

    fun isFileInCache(messageContent: MessageWithAttachmentContent) =
            isFileInCache(
                    mxcUrl = messageContent.getFileUrl(),
                    fileName = messageContent.getFileName(),
                    mimeType = messageContent.mimeType,
                    elementToDecrypt = messageContent.encryptedFileInfo?.toElementToDecrypt()
            )

    /**
     * Use this URI and pass it to intent using flag Intent.FLAG_GRANT_READ_URI_PERMISSION
     * (if not other app won't be able to access it).
     */
    fun getTemporarySharableURI(
            mxcUrl: String?,
            fileName: String,
            mimeType: String?,
            elementToDecrypt: ElementToDecrypt?
    ): Uri?

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
    fun fileState(
            mxcUrl: String?,
            fileName: String,
            mimeType: String?,
            elementToDecrypt: ElementToDecrypt?
    ): FileState

    fun fileState(messageContent: MessageWithAttachmentContent): FileState =
            fileState(
                    mxcUrl = messageContent.getFileUrl(),
                    fileName = messageContent.getFileName(),
                    mimeType = messageContent.mimeType,
                    elementToDecrypt = messageContent.encryptedFileInfo?.toElementToDecrypt()
            )

    /**
     * Clears all the files downloaded by the service, including decrypted files.
     */
    fun clearCache()

    /**
     * Clears all the decrypted files by the service.
     */
    fun clearDecryptedCache()

    /**
     * Get size of cached files.
     */
    fun getCacheSize(): Long
}
