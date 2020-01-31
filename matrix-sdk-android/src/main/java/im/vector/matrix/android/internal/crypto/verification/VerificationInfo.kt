/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.internal.crypto.verification

import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.internal.crypto.model.rest.SendToDeviceObject

interface VerificationInfo {
    fun toEventContent(): Content? = null
    fun toSendToDeviceObject(): SendToDeviceObject? = null
    fun isValid(): Boolean

    /**
     * String to identify the transaction.
     * This string must be unique for the pair of users performing verification for the duration that the transaction is valid.
     * Alice’s device should record this ID and use it in future messages in this transaction.
     */
    val transactionID: String?

    // TODO Refacto Put the relatesTo here or at least in Message sent in Room parent?
    // val relatesTo: RelationDefaultContent?
}
