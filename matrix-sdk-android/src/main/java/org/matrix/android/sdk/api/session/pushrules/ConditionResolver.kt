/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.pushrules

import org.matrix.android.sdk.api.session.events.model.Event

/**
 * Acts like a visitor on Conditions.
 * This class as all required context needed to evaluate rules
 */
interface ConditionResolver {
    fun resolveEventMatchCondition(
            event: Event,
            condition: EventMatchCondition
    ): Boolean

    fun resolveRoomMemberCountCondition(
            event: Event,
            condition: RoomMemberCountCondition
    ): Boolean

    fun resolveSenderNotificationPermissionCondition(
            event: Event,
            condition: SenderNotificationPermissionCondition
    ): Boolean

    fun resolveContainsDisplayNameCondition(
            event: Event,
            condition: ContainsDisplayNameCondition
    ): Boolean
}
