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
internal open class SpaceParentSummaryEntity(
        /**
         * Determines whether this is the main parent for the space
         * When a user joins a room with a canonical parent, clients may switch to view the room in the context of that space,
         * peeking into it in order to find other rooms and group them together.
         * In practice, well behaved rooms should only have one canonical parent, but given this is not enforced:
         * if multiple are present the client should select the one with the lowest room ID,
         * as determined via a lexicographic utf-8 ordering.
         */
        var canonical: Boolean? = null,

        var parentRoomId: String? = null,
        // Link to the actual space summary if it is known locally
        var parentSummaryEntity: RoomSummaryEntity? = null,

        var viaServers: RealmList<String> = RealmList()

) : RealmObject() {

    companion object
}
