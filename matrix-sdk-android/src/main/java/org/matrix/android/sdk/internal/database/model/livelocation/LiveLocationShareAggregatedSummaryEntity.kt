/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
