/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room

import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.getRelationContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.events.model.toValidDecryptedEvent
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.internal.crypto.store.IMXCommonCryptoStore
import javax.inject.Inject

internal class EventEditValidator @Inject constructor(val cryptoStore: IMXCommonCryptoStore) {

    sealed class EditValidity {
        object Valid : EditValidity()
        data class Invalid(val reason: String) : EditValidity()
        object Unknown : EditValidity()
    }

    /**
     * There are a number of requirements on replacement events, which must be satisfied for the replacement
     * to be considered valid:
     * As with all event relationships, the original event and replacement event must have the same room_id
     * (i.e. you cannot send an event in one room and then an edited version in a different room).
     * The original event and replacement event must have the same sender (i.e. you cannot edit someone else’s messages).
     * The replacement and original events must have the same type (i.e. you cannot change the original event’s type).
     * The replacement and original events must not have a state_key property (i.e. you cannot edit state events at all).
     * The original event must not, itself, have a rel_type of m.replace
     * (i.e. you cannot edit an edit — though you can send multiple edits for a single original event).
     * The replacement event (once decrypted, if appropriate) must have an m.new_content property.
     *
     * If the original event was encrypted, the replacement should be too.
     */
    fun validateEdit(originalEvent: Event?, replaceEvent: Event): EditValidity {
        // we might not know the original event at that time. In this case we can't perform the validation
        // Edits should be revalidated when the original event is received
        if (originalEvent == null) {
            return EditValidity.Unknown
        }

        if (LocalEcho.isLocalEchoId(replaceEvent.eventId.orEmpty())) {
            // Don't validate local echo
            return EditValidity.Unknown
        }

        if (originalEvent.roomId != replaceEvent.roomId) {
            return EditValidity.Invalid("original event and replacement event must have the same room_id")
        }
        if (originalEvent.isStateEvent() || replaceEvent.isStateEvent()) {
            return EditValidity.Invalid("replacement and original events must not have a state_key property")
        }
        // check it's from same sender

        if (originalEvent.isEncrypted()) {
            if (!replaceEvent.isEncrypted()) return EditValidity.Invalid("If the original event was encrypted, the replacement should be too")
            val originalDecrypted = originalEvent.toValidDecryptedEvent()
                    ?: return EditValidity.Unknown // UTD can't decide
            val replaceDecrypted = replaceEvent.toValidDecryptedEvent()
                    ?: return EditValidity.Unknown // UTD can't decide

            if (originalEvent.senderId != replaceEvent.senderId) {
                return EditValidity.Invalid("original event and replacement event must have the same sender")
            }

            val originalSendingDevice = originalEvent.senderId?.let { cryptoStore.deviceWithIdentityKey(it, originalDecrypted.cryptoSenderKey) }
            val editSendingDevice = originalEvent.senderId?.let { cryptoStore.deviceWithIdentityKey(it, replaceDecrypted.cryptoSenderKey) }

            if (originalDecrypted.getRelationContent()?.type == RelationType.REPLACE) {
                return EditValidity.Invalid("The original event must not, itself, have a rel_type of m.replace ")
            }

            if (originalSendingDevice == null || editSendingDevice == null) {
                // mm what can we do? we don't know if it's cryptographically from same user?
                // maybe it's a deleted device or a not yet downloaded one?
                return EditValidity.Unknown
            }

            if (originalDecrypted.type != replaceDecrypted.type) {
                return EditValidity.Invalid("replacement and original events must have the same type")
            }
            if (!hasNewContent(replaceDecrypted.type, replaceDecrypted.clearContent)) {
                return EditValidity.Invalid("replacement event must have an m.new_content property")
            }
        } else {
            if (originalEvent.getRelationContent()?.type == RelationType.REPLACE) {
                return EditValidity.Invalid("The original event must not, itself, have a rel_type of m.replace ")
            }

            // check the sender
            if (originalEvent.senderId != replaceEvent.senderId) {
                return EditValidity.Invalid("original event and replacement event must have the same sender")
            }
            if (originalEvent.type != replaceEvent.type) {
                return EditValidity.Invalid("replacement and original events must have the same type")
            }
            if (!hasNewContent(replaceEvent.type, replaceEvent.content)) {
                return EditValidity.Invalid("replacement event must have an m.new_content property")
            }
        }

        return EditValidity.Valid
    }

    private fun hasNewContent(eventType: String?, content: Content?): Boolean {
        return when (eventType) {
            in EventType.POLL_START.values -> content.toModel<MessagePollContent>()?.newContent != null
            else -> content.toModel<MessageContent>()?.newContent != null
        }
    }
}
