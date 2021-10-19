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

package org.matrix.android.sdk.internal.crypto.attachments

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.internal.crypto.model.rest.EncryptedFileInfo

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
 * Represent data to decode an attachment
 */
@Parcelize
data class ElementToDecrypt(
        val iv: String,
        val k: String,
        val sha256: String
) : Parcelable
