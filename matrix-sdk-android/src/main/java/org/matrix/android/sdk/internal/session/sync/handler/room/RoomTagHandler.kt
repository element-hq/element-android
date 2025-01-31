/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync.handler.room

import io.realm.Realm
import org.matrix.android.sdk.api.session.room.model.tag.RoomTagContent
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomTagEntity
import org.matrix.android.sdk.internal.database.query.getOrCreate
import javax.inject.Inject

internal class RoomTagHandler @Inject constructor() {

    fun handle(realm: Realm, roomId: String, content: RoomTagContent?) {
        if (content == null) {
            return
        }
        val tags = content.tags.entries.map { (tagName, params) ->
            RoomTagEntity(tagName, params["order"] as? Double)
            Pair(tagName, params["order"] as? Double)
        }
        RoomSummaryEntity.getOrCreate(realm, roomId).updateTags(tags)
    }
}
