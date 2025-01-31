/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.events.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * <code>
 *  {
 *       "m.annotation": {
 *          "chunk": [
 *               {
 *                   "type": "m.reaction",
 *                   "key": "👍",
 *                   "count": 3
 *               }
 *              ],
 *              "limited": false,
 *              "count": 1
 *       },
 *       "m.reference": {
 *           "chunk": [
 *               {
 *                  "type": "m.room.message",
 *                  "event_id": "$some_event_id"
 *              }
 *           ],
 *           "limited": false,
 *           "count": 1
 *           }
 *      }
 * </code>
 */

@JsonClass(generateAdapter = true)
data class AggregatedRelations(
        @Json(name = "m.annotation") val annotations: AggregatedAnnotation? = null,
        @Json(name = "m.reference") val references: DefaultUnsignedRelationInfo? = null,
        @Json(name = RelationType.THREAD) val latestThread: LatestThreadUnsignedRelation? = null
)
