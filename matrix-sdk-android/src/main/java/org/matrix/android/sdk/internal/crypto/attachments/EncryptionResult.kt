/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.attachments

import org.matrix.android.sdk.api.session.crypto.model.EncryptedFileInfo

/**
 * Define the result of an encryption file.
 */
internal data class EncryptionResult(
        val encryptedFileInfo: EncryptedFileInfo,
        val encryptedByteArray: ByteArray
)
