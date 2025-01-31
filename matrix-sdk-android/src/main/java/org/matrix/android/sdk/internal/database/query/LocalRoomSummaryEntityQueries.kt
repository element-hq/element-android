/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.query

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.LocalRoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.LocalRoomSummaryEntityFields

internal fun LocalRoomSummaryEntity.Companion.where(realm: Realm, roomId: String): RealmQuery<LocalRoomSummaryEntity> {
    return realm.where<LocalRoomSummaryEntity>().equalTo(LocalRoomSummaryEntityFields.ROOM_ID, roomId)
}
