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
import io.realm.annotations.PrimaryKey

internal open class ReadReceiptsSummaryEntity(
        @PrimaryKey
        var eventId: String = "",
        var roomId: String = "",
        var readReceipts: RealmList<ReadReceiptEntity> = RealmList()
) : RealmObject() {

    @LinkingObjects("readReceipts")
    val timelineEvent: RealmResults<TimelineEventEntity>? = null

    companion object
}

internal fun ReadReceiptsSummaryEntity.deleteOnCascade() {
    readReceipts.deleteAllFromRealm()
    deleteFromRealm()
}
