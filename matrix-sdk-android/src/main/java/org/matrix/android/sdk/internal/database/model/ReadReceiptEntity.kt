/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.model

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

internal open class ReadReceiptEntity(
        @PrimaryKey var primaryKey: String = "",
        var eventId: String = "",
        var roomId: String = "",
        var userId: String = "",
        var originServerTs: Double = 0.0
) : RealmObject() {
    companion object

    @LinkingObjects("readReceipts")
    val summary: RealmResults<ReadReceiptsSummaryEntity>? = null
}
