/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.state

import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomJoinRulesContent

/**
 * Return true if a room can be joined by anyone (RoomJoinRules.PUBLIC).
 */
fun StateService.isPublic(): Boolean {
    return getStateEvent(EventType.STATE_ROOM_JOIN_RULES, QueryStringValue.IsEmpty)
            ?.content
            ?.toModel<RoomJoinRulesContent>()
            ?.joinRules == RoomJoinRules.PUBLIC
}
