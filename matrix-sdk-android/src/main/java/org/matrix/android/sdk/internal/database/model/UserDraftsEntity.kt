/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects

/**
 * Create a specific table to be able to do direct query on it and keep the draft ordered.
 */
internal open class UserDraftsEntity(
        var userDrafts: RealmList<DraftEntity> = RealmList()
) : RealmObject() {

    // Link to RoomSummaryEntity
    @LinkingObjects("userDrafts")
    val roomSummaryEntity: RealmResults<RoomSummaryEntity>? = null

    companion object
}
