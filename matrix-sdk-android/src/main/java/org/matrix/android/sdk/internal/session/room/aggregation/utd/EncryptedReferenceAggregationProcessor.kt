/*
 * Copyright (c) 2022 New Vector Ltd
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

package org.matrix.android.sdk.internal.session.room.aggregation.utd

import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.Event
import javax.inject.Inject

class EncryptedReferenceAggregationProcessor @Inject constructor() {

    // TODO add unit tests
    fun handle(
            realm: Realm,
            event: Event,
            roomId: String,
            isLocalEcho: Boolean,
            relatedEventId: String?
    ) {
        if(isLocalEcho || relatedEventId.isNullOrEmpty()) return

        handlePollReference(realm = realm, event = event, roomId = roomId, relatedEventId = relatedEventId)
    }

    // TODO how to check this is working?
    private fun handlePollReference(
            realm: Realm,
            event: Event,
            roomId: String,
            relatedEventId: String
    ) {
        // TODO check if relatedEventId is referencing any existing poll event in DB
        // TODO if related to a poll, then add the event id into the list of encryptedRelatedEvents in the summary
    }
}
