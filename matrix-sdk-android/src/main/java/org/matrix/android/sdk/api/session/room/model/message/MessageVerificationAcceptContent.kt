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
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoAccept
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoAcceptFactory

@JsonClass(generateAdapter = true)
internal data class MessageVerificationAcceptContent(
        @Json(name = "hash") override val hash: String?,
        @Json(name = "key_agreement_protocol") override val keyAgreementProtocol: String?,
        @Json(name = "message_authentication_code") override val messageAuthenticationCode: String?,
        @Json(name = "short_authentication_string") override val shortAuthenticationStrings: List<String>?,
        @Json(name = "m.relates_to") val relatesTo: RelationDefaultContent?,
        @Json(name = "commitment") override var commitment: String? = null
) : VerificationInfoAccept {

    override val transactionId: String?
        get() = relatesTo?.eventId

    override fun toEventContent() = toContent()

    companion object : VerificationInfoAcceptFactory {

        override fun create(tid: String,
                            keyAgreementProtocol: String,
                            hash: String,
                            commitment: String,
                            messageAuthenticationCode: String,
                            shortAuthenticationStrings: List<String>): VerificationInfoAccept {
            return MessageVerificationAcceptContent(
                    hash,
                    keyAgreementProtocol,
                    messageAuthenticationCode,
                    shortAuthenticationStrings,
                    RelationDefaultContent(
                            RelationType.REFERENCE,
                            tid
                    ),
                    commitment
            )
        }
    }
}
