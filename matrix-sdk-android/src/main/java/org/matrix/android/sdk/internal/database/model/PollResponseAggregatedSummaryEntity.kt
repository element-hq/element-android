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
 * Keep the latest state of a poll.
 */
internal open class PollResponseAggregatedSummaryEntity(
        // For now we persist this a JSON for greater flexibility
        // #see PollSummaryContent
        var aggregatedContent: String? = null,

        // If set the poll is closed (Clients SHOULD NOT consider responses after the close event)
        var closedTime: Long? = null,
        // Clients SHOULD validate that the option in the relationship is a valid option, and ignore the response if invalid
        var nbOptions: Int = 0,

        // The list of the eventIDs used to build the summary (might be out of sync if chunked received from message chunk)
        var sourceEvents: RealmList<String> = RealmList(),
        var sourceLocalEchoEvents: RealmList<String> = RealmList()
) : RealmObject() {

    companion object
}
