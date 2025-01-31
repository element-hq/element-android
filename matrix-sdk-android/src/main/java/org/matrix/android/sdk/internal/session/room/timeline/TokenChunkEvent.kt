/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.timeline

import org.matrix.android.sdk.api.session.events.model.Event

internal interface TokenChunkEvent {
    val start: String?
    val end: String?
    val events: List<Event>
    val stateEvents: List<Event>?

    fun hasMore() = end != null && start != end
}
