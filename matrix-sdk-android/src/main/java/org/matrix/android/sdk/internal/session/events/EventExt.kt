/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.events

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent

internal fun Event.getFixedRoomMemberContent(): RoomMemberContent? {
    val content = content.toModel<RoomMemberContent>()
    // if user is leaving, we should grab his last name and avatar from prevContent
    return if (content?.membership?.isLeft() == true) {
        val prevContent = resolvedPrevContent().toModel<RoomMemberContent>()
        content.copy(
                displayName = prevContent?.displayName,
                avatarUrl = prevContent?.avatarUrl
        )
    } else {
        content
    }
}
