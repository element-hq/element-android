/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.model

import io.realm.RealmObject

/**
 * Contain a map between Json filter string and filterId (from Homeserver).
 * Currently there is only one object in this table.
 */
internal open class FilterEntity(
        // The serialized FilterBody
        var filterBodyJson: String = "",
        // The serialized room event filter for pagination
        var roomEventFilterJson: String = "",
        // the id server side of the filterBodyJson, can be used instead of filterBodyJson if not blank
        var filterId: String = ""

) : RealmObject() {

    companion object
}
