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

package org.matrix.android.sdk.internal.session.room.aggregation.livelocation

import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationBeaconContent
import org.matrix.android.sdk.api.session.room.model.message.MessageLiveLocationContent
import javax.inject.Inject

internal class DefaultLiveLocationAggregationProcessor @Inject constructor() : LiveLocationAggregationProcessor {

    override fun handleBeaconInfo(realm: Realm, event: Event, content: LiveLocationBeaconContent, roomId: String, isLocalEcho: Boolean) {
        //val locationSenderId = event.senderId ?: return

        // We shouldn't process local echos
        if (isLocalEcho) {
            return
        }

        // TODO if live field is true, get eventId else get get replace eventId
        // TODO getOrCreate existing aggregated summary
        // TODO update the endOfLiveTimestamp and live fields
    }

    override fun handleLiveLocation(realm: Realm, event: Event, content: MessageLiveLocationContent, roomId: String, isLocalEcho: Boolean) {
        //val locationSenderId = event.senderId ?: return

        // We shouldn't process local echos
        if (isLocalEcho) {
            return
        }

        // TODO getOrCreate existing aggregated summary
        // TODO add location content only if more recent than the current one if any
    }
}
