/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.message

import org.matrix.android.sdk.api.session.crypto.model.EncryptedFileInfo

/**
 * Interface for message which can contains an encrypted file.
 */
interface MessageWithAttachmentContent : MessageContent {
    /**
     * Required if the file is unencrypted. The URL (typically MXC URI) to the image.
     */
    val url: String?

    /**
     * Required if the file is encrypted. Information on the encrypted file, as specified in End-to-end encryption.
     */
    val encryptedFileInfo: EncryptedFileInfo?

    val mimeType: String?
}

/**
 * Get the url of the encrypted file or of the file.
 */
fun MessageWithAttachmentContent.getFileUrl() = encryptedFileInfo?.url ?: url

fun MessageWithAttachmentContent.getFileName() = (this as? MessageFileContent)?.getFileName() ?: body
