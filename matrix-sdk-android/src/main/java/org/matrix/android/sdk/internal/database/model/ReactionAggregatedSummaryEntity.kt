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
 * Aggregated Summary of a reaction.
 */
internal open class ReactionAggregatedSummaryEntity(
        // The reaction String 😀
        var key: String = "",
        // Number of time this reaction was selected
        var count: Int = 0,
        // Did the current user sent this reaction
        var addedByMe: Boolean = false,
        // The first time this reaction was added (for ordering purpose)
        var firstTimestamp: Long = 0,
        // The list of the eventIDs used to build the summary (might be out of sync if chunked received from message chunk)
        var sourceEvents: RealmList<String> = RealmList(),
        // List of transaction ids for local echos
        var sourceLocalEcho: RealmList<String> = RealmList()
) : RealmObject() {

    companion object
}
