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
import io.realm.annotations.PrimaryKey
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntity
import org.matrix.android.sdk.internal.database.query.findRootOrLatest
import org.matrix.android.sdk.internal.extensions.assertIsManaged

internal open class RoomEntity(
        @PrimaryKey var roomId: String = "",
        var chunks: RealmList<ChunkEntity> = RealmList(),
        var sendingTimelineEvents: RealmList<TimelineEventEntity> = RealmList(),
        var threadSummaries: RealmList<ThreadSummaryEntity> = RealmList(),
        var accountData: RealmList<RoomAccountDataEntity> = RealmList()
) : RealmObject() {

    private var membershipStr: String = Membership.NONE.name
    var membership: Membership
        get() {
            return Membership.valueOf(membershipStr)
        }
        set(value) {
            membershipStr = value.name
        }

    private var membersLoadStatusStr: String = RoomMembersLoadStatusType.NONE.name
    var membersLoadStatus: RoomMembersLoadStatusType
        get() {
            return RoomMembersLoadStatusType.valueOf(membersLoadStatusStr)
        }
        set(value) {
            membersLoadStatusStr = value.name
        }

    companion object
}

internal fun RoomEntity.removeThreadSummaryIfNeeded(eventId: String) {
    assertIsManaged()
    threadSummaries.findRootOrLatest(eventId)?.let {
        threadSummaries.remove(it)
        it.deleteFromRealm()
    }
}
