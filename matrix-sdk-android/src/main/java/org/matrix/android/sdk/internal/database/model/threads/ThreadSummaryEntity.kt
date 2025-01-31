/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.model.threads

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.RoomEntity

internal open class ThreadSummaryEntity(
        @Index var rootThreadEventId: String? = "",
        var rootThreadEventEntity: EventEntity? = null,
        var latestThreadEventEntity: EventEntity? = null,
        var rootThreadSenderName: String? = null,
        var latestThreadSenderName: String? = null,
        var rootThreadSenderAvatar: String? = null,
        var latestThreadSenderAvatar: String? = null,
        var rootThreadIsUniqueDisplayName: Boolean = false,
        var isUserParticipating: Boolean = false,
        var latestThreadIsUniqueDisplayName: Boolean = false,
        var numberOfThreads: Int = 0
) : RealmObject() {

    @LinkingObjects("threadSummaries")
    val room: RealmResults<RoomEntity>? = null

    companion object
}
