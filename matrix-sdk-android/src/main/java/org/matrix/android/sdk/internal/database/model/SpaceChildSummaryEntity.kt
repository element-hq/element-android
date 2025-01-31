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

/**
 * Decorates room summary with space related information.
 */
internal open class SpaceChildSummaryEntity(
//        var isSpace: Boolean = false,

        var order: String? = null,

        var autoJoin: Boolean? = null,

        var suggested: Boolean? = null,

        var childRoomId: String? = null,
        // Link to the actual space summary if it is known locally
        var childSummaryEntity: RoomSummaryEntity? = null,

        var viaServers: RealmList<String> = RealmList()
//        var owner: RoomSummaryEntity? = null,

//        var level: Int = 0

) : RealmObject() {

    companion object
}
