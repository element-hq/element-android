/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.contentscanner

import org.matrix.android.sdk.api.session.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.api.session.crypto.model.EncryptedFileInfo
import org.matrix.android.sdk.api.session.crypto.model.EncryptedFileKey
import org.matrix.android.sdk.internal.crypto.tools.withOlmEncryption
import org.matrix.android.sdk.internal.session.contentscanner.model.DownloadBody
import org.matrix.android.sdk.internal.session.contentscanner.model.EncryptedBody
import org.matrix.android.sdk.internal.session.contentscanner.model.toCanonicalJson

internal object ScanEncryptorUtils {

    fun getDownloadBodyAndEncryptIfNeeded(publicServerKey: String?, mxcUrl: String, elementToDecrypt: ElementToDecrypt): DownloadBody {
        // TODO, upstream refactoring changed the object model here...
        // it's bad we have to recreate and use hardcoded values
        val encryptedInfo = EncryptedFileInfo(
                url = mxcUrl,
                iv = elementToDecrypt.iv,
                hashes = mapOf("sha256" to elementToDecrypt.sha256),
                key = EncryptedFileKey(
                        k = elementToDecrypt.k,
                        alg = "A256CTR",
                        keyOps = listOf("encrypt", "decrypt"),
                        kty = "oct",
                        ext = true
                ),
                v = "v2"
        )
        return if (publicServerKey != null) {
            // We should encrypt
            withOlmEncryption { olm ->
                olm.setRecipientKey(publicServerKey)

                val olmResult = olm.encrypt(DownloadBody(encryptedInfo).toCanonicalJson())
                DownloadBody(
                        encryptedBody = EncryptedBody(
                                cipherText = olmResult.mCipherText,
                                ephemeral = olmResult.mEphemeralKey,
                                mac = olmResult.mMac
                        )
                )
            }
        } else {
            DownloadBody(encryptedInfo)
        }
    }
}
