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
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationBeaconContent
import org.matrix.android.sdk.api.session.room.model.message.MessageLiveLocationContent
import org.matrix.android.sdk.internal.database.mapper.ContentMapper
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.query.getOrNull
import timber.log.Timber
import javax.inject.Inject

internal class DefaultLiveLocationAggregationProcessor @Inject constructor() : LiveLocationAggregationProcessor {

    override fun handleLiveLocation(realm: Realm, event: Event, content: MessageLiveLocationContent, roomId: String, isLocalEcho: Boolean) {
        val locationSenderId = event.senderId ?: return

        // We shouldn't process local echos
        if (isLocalEcho) {
            return
        }

        // A beacon info state event has to be sent before sending location
        var beaconInfoEntity: CurrentStateEventEntity? = null
        val eventTypesIterator = EventType.STATE_ROOM_BEACON_INFO.iterator()
        while (beaconInfoEntity == null && eventTypesIterator.hasNext()) {
            beaconInfoEntity = CurrentStateEventEntity.getOrNull(realm, roomId, locationSenderId, eventTypesIterator.next())
        }

        if (beaconInfoEntity == null) {
            Timber.v("## LIVE LOCATION. There is not any beacon info which should be emitted before sending location updates")
            return
        }
        val beaconInfoContent = ContentMapper.map(beaconInfoEntity.root?.content)?.toModel<LiveLocationBeaconContent>(catchError = true)
        if (beaconInfoContent == null) {
            Timber.v("## LIVE LOCATION. Beacon info content is invalid")
            return
        }

        // Check if live location is ended
        if (!beaconInfoContent.getBestBeaconInfo()?.isLive.orFalse()) {
            Timber.v("## LIVE LOCATION. Beacon info is not live anymore")
            return
        }

        // Check if beacon info is outdated
        if (isBeaconInfoOutdated(beaconInfoContent, content)) {
            Timber.v("## LIVE LOCATION. Beacon info has timeout")
            return
        }

        // Update last location info of the beacon state event
        beaconInfoContent.lastLocationContent = content
        beaconInfoEntity.root?.content = ContentMapper.map(beaconInfoContent.toContent())
    }

    private fun isBeaconInfoOutdated(beaconInfoContent: LiveLocationBeaconContent,
                                     liveLocationContent: MessageLiveLocationContent): Boolean {
        val beaconInfoStartTime = beaconInfoContent.getBestTimestampAsMilliseconds() ?: 0
        val liveLocationEventTime = liveLocationContent.getBestTimestampAsMilliseconds() ?: 0
        val timeout = beaconInfoContent.getBestBeaconInfo()?.timeout ?: 0
        return liveLocationEventTime - beaconInfoStartTime > timeout
    }
}
