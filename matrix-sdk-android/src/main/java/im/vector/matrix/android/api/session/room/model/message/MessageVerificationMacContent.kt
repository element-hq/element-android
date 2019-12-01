/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.RelationType
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.room.model.relation.RelationDefaultContent
import im.vector.matrix.android.internal.crypto.verification.VerifInfoMac
import im.vector.matrix.android.internal.crypto.verification.VerifInfoMacFactory

@JsonClass(generateAdapter = true)
internal data class MessageVerificationMacContent(
        @Json(name = "mac") override val mac: Map<String, String>? = null,
        @Json(name = "keys") override val keys: String? = null,
        @Json(name = "m.relates_to") val relatesTo: RelationDefaultContent?
) : VerifInfoMac {

    override val transactionID: String?
        get() = relatesTo?.eventId

    override fun toEventContent() = this.toContent()

    override fun isValid(): Boolean {
        if (transactionID.isNullOrBlank() || keys.isNullOrBlank() || mac.isNullOrEmpty()) {
            return false
        }
        return true
    }

    companion object : VerifInfoMacFactory {
        override fun create(tid: String, mac: Map<String, String>, keys: String): VerifInfoMac {
            return MessageVerificationMacContent(
                    mac,
                    keys,
                    RelationDefaultContent(
                            RelationType.REFERENCE,
                            tid
                    )
            )
        }
    }
}
