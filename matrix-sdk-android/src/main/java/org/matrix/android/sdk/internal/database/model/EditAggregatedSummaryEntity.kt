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
import io.realm.annotations.RealmClass

/**
 * Keep all the editions of a message.
 */
internal open class EditAggregatedSummaryEntity(
        // The list of the editions used to build the summary (might be out of sync if chunked received from message chunk)
        var editions: RealmList<EditionOfEvent> = RealmList()
) : RealmObject() {

    companion object
}

@RealmClass(embedded = true)
internal open class EditionOfEvent(
        var senderId: String = "",
        var eventId: String = "",
        var content: String? = null,
        var timestamp: Long = 0,
        var isLocalEcho: Boolean = false
) : RealmObject()
