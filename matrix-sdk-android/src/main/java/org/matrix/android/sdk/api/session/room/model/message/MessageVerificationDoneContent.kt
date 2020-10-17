/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfo

@JsonClass(generateAdapter = true)
internal data class MessageVerificationDoneContent(
        @Json(name = "m.relates_to") val relatesTo: RelationDefaultContent?
) : VerificationInfo<ValidVerificationDone> {

    override val transactionId: String?
        get() = relatesTo?.eventId

    override fun toEventContent(): Content? = toContent()

    override fun asValidObject(): ValidVerificationDone? {
        val validTransactionId = transactionId?.takeIf { it.isNotEmpty() } ?: return null

        return ValidVerificationDone(
                validTransactionId
        )
    }
}

internal data class ValidVerificationDone(
        val transactionId: String
)
