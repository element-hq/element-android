/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent

internal object IsUselessResolver {

    /**
     * @return true if the event is useless
     */
    fun isUseless(event: Event): Boolean {
        return when (event.type) {
            EventType.STATE_ROOM_MEMBER -> {
                // Call toContent(), to filter out null value
                event.content != null &&
                        event.content.toContent() == event.resolvedPrevContent()?.toContent()
            }
            else -> false
        }
    }
}
