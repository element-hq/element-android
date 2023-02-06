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

package org.matrix.android.sdk.internal.database.model.livelocation

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Aggregation info concerning a live location share.
 */
internal open class LiveLocationShareAggregatedSummaryEntity(
        /**
         * Event id of the event that started the live.
         */
        @PrimaryKey
        var eventId: String = "",

        /**
         * List of event ids used to compute the aggregated summary data.
         */
        var relatedEventIds: RealmList<String> = RealmList(),

        var roomId: String = "",

        var userId: String = "",

        /**
         * Indicate whether the live is currently running.
         */
        var isActive: Boolean? = null,

        var startOfLiveTimestampMillis: Long? = null,

        var endOfLiveTimestampMillis: Long? = null,

        /**
         * For now we persist this as a JSON for greater flexibility.
         * @see [org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent]
         */
        var lastLocationContent: String? = null,
) : RealmObject() {
    companion object
}
