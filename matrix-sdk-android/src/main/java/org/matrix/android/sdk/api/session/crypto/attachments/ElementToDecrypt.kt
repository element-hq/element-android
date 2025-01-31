/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.attachments

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.crypto.model.EncryptedFileInfo

fun EncryptedFileInfo.toElementToDecrypt(): ElementToDecrypt? {
    // Check the validity of some fields
    if (isValid()) {
        // It's valid so the data are here
        return ElementToDecrypt(
                iv = this.iv ?: "",
                k = this.key?.k ?: "",
                sha256 = this.hashes?.get("sha256") ?: ""
        )
    }

    return null
}

/**
 * Represent data to decode an attachment.
 */
@Parcelize
data class ElementToDecrypt(
        val iv: String,
        val k: String,
        val sha256: String
) : Parcelable
