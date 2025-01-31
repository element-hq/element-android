/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync.handler.room

import io.realm.Realm
import org.matrix.android.sdk.internal.database.model.ReadMarkerEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.session.room.read.FullyReadContent
import timber.log.Timber
import javax.inject.Inject

internal class RoomFullyReadHandler @Inject constructor() {

    fun handle(realm: Realm, roomId: String, content: FullyReadContent?) {
        if (content == null) {
            return
        }
        Timber.v("Handle for roomId: $roomId eventId: ${content.eventId}")

        RoomSummaryEntity.getOrCreate(realm, roomId).apply {
            readMarkerId = content.eventId
        }
        ReadMarkerEntity.getOrCreate(realm, roomId).apply {
            this.eventId = content.eventId
        }
    }
}
