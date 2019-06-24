/*
 * Copyright 2016 OpenMarket Ltd
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

package im.vector.matrix.android.internal.crypto.attachments

import android.os.Parcelable
import im.vector.matrix.android.internal.crypto.model.rest.EncryptedFileInfo
import kotlinx.android.parcel.Parcelize


fun EncryptedFileInfo.toElementToDecrypt(): ElementToDecrypt? {
    // Check the validity of some fields
    if (isValid()) {
        return ElementToDecrypt(
                iv = this.iv!!,
                k = this.key!!.k!!,
                sha256 = this.hashes!!["sha256"] ?: error("")
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