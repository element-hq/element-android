/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session

import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.database.model.EventInsertType

internal interface EventInsertLiveProcessor {

    fun shouldProcess(eventId: String, eventType: String, insertType: EventInsertType): Boolean

    suspend fun process(realm: Realm, event: Event)

    /**
     * Called after transaction.
     * Maybe you prefer to process the events outside of the realm transaction.
     */
    suspend fun onPostProcess() {
        // Noop by default
    }
}
