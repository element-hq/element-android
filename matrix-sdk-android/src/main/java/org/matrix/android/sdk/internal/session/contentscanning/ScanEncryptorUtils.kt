/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package org.matrix.android.sdk.internal.session.contentscanning

import org.matrix.android.sdk.internal.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.internal.crypto.model.rest.EncryptedFileInfo
import org.matrix.android.sdk.internal.crypto.model.rest.EncryptedFileKey
import org.matrix.android.sdk.internal.crypto.tools.withOlmEncryption
import org.matrix.android.sdk.internal.session.contentscanning.model.DownloadBody
import org.matrix.android.sdk.internal.session.contentscanning.model.EncryptedBody
import org.matrix.android.sdk.internal.session.contentscanning.model.toCanonicalJson

object ScanEncryptorUtils {

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
