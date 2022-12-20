/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.internal.session.UnableToDecryptEventLiveProcessor
import org.matrix.android.sdk.internal.session.room.aggregation.utd.EncryptedReferenceAggregationProcessor
import timber.log.Timber
import javax.inject.Inject

internal class EncryptedEventRelationsAggregationProcessor @Inject constructor(
        private val encryptedReferenceAggregationProcessor: EncryptedReferenceAggregationProcessor,
) : UnableToDecryptEventLiveProcessor {

    // TODO add unit tests
    override fun process(realm: Realm, event: Event) {
        val roomId = event.roomId
        if (roomId == null) {
            Timber.w("Event has no room id ${event.eventId}")
            return
        }

        val isLocalEcho = LocalEcho.isLocalEchoId(event.eventId ?: "")

        when (event.getClearType()) {
            EventType.ENCRYPTED -> {
                val encryptedEventContent = event.content.toModel<EncryptedEventContent>()
                processEncryptedContent(
                        encryptedEventContent = encryptedEventContent,
                        realm = realm,
                        event = event,
                        roomId = roomId,
                        isLocalEcho = isLocalEcho,
                )
            }
            else -> Unit
        }
    }

    private fun processEncryptedContent(
            encryptedEventContent: EncryptedEventContent?,
            realm: Realm,
            event: Event,
            roomId: String,
            isLocalEcho: Boolean,
    ) {
        when (encryptedEventContent?.relatesTo?.type) {
            RelationType.REPLACE -> {
                Timber.w("## UTD replace in room $roomId for event ${event.eventId}")
            }
            RelationType.RESPONSE -> {
                // can we / should we do we something for UTD response??
                Timber.w("## UTD response in room $roomId related to ${encryptedEventContent.relatesTo.eventId}")
            }
            RelationType.REFERENCE -> {
                // can we / should we do we something for UTD reference??
                Timber.w("## UTD reference in room $roomId related to ${encryptedEventContent.relatesTo.eventId}")
                encryptedReferenceAggregationProcessor.handle(realm, event, roomId, isLocalEcho, encryptedEventContent.relatesTo.eventId)
            }
            RelationType.ANNOTATION -> {
                // can we / should we do we something for UTD annotation??
                Timber.w("## UTD annotation in room $roomId related to ${encryptedEventContent.relatesTo.eventId}")
            }
            else -> Unit
        }
    }
}
