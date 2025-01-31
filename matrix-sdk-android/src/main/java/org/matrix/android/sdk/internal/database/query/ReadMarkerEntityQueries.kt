/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.query

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.ReadMarkerEntity
import org.matrix.android.sdk.internal.database.model.ReadMarkerEntityFields

internal fun ReadMarkerEntity.Companion.where(realm: Realm, roomId: String): RealmQuery<ReadMarkerEntity> {
    return realm.where<ReadMarkerEntity>()
            .equalTo(ReadMarkerEntityFields.ROOM_ID, roomId)
}

internal fun ReadMarkerEntity.Companion.getOrCreate(realm: Realm, roomId: String): ReadMarkerEntity {
    return where(realm, roomId).findFirst() ?: realm.createObject(roomId)
}
