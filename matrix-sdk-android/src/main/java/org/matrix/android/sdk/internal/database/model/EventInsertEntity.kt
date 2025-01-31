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
 * This class is used to get notification on new events being inserted. It's to avoid realm getting slow when listening to insert
 * in EventEntity table.
 */
internal open class EventInsertEntity(
        var eventId: String = "",
        var eventType: String = "",
        /**
         * This flag will be used to filter EventInsertEntity in EventInsertLiveObserver.
         * Currently it's set to false when the event content is encrypted.
         */
        var canBeProcessed: Boolean = true
) : RealmObject() {

    private var insertTypeStr: String = EventInsertType.INCREMENTAL_SYNC.name
    var insertType: EventInsertType
        get() {
            return EventInsertType.valueOf(insertTypeStr)
        }
        set(value) {
            insertTypeStr = value.name
        }
}
