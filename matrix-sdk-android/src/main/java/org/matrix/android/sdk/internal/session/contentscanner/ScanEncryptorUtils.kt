/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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
