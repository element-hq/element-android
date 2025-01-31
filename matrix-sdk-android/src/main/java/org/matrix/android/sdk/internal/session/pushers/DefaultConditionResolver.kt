/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.pushers

import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.pushrules.ConditionResolver
import org.matrix.android.sdk.api.session.pushrules.ContainsDisplayNameCondition
import org.matrix.android.sdk.api.session.pushrules.EventMatchCondition
import org.matrix.android.sdk.api.session.pushrules.RoomMemberCountCondition
import org.matrix.android.sdk.api.session.pushrules.SenderNotificationPermissionCondition
import org.matrix.android.sdk.api.session.room.getStateEvent
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.room.RoomGetter
import javax.inject.Inject

internal class DefaultConditionResolver @Inject constructor(
        private val roomGetter: RoomGetter,
        @UserId private val userId: String
) : ConditionResolver {

    override fun resolveEventMatchCondition(
            event: Event,
            condition: EventMatchCondition
    ): Boolean {
        return condition.isSatisfied(event)
    }

    override fun resolveRoomMemberCountCondition(
            event: Event,
            condition: RoomMemberCountCondition
    ): Boolean {
        return condition.isSatisfied(event, roomGetter)
    }

    override fun resolveSenderNotificationPermissionCondition(
            event: Event,
            condition: SenderNotificationPermissionCondition
    ): Boolean {
        val roomId = event.roomId ?: return false
        val room = roomGetter.getRoom(roomId) ?: return false

        val powerLevelsContent = room.getStateEvent(EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.IsEmpty)
                ?.content
                ?.toModel<PowerLevelsContent>()
                ?: PowerLevelsContent()

        return condition.isSatisfied(event, powerLevelsContent)
    }

    override fun resolveContainsDisplayNameCondition(
            event: Event,
            condition: ContainsDisplayNameCondition
    ): Boolean {
        val roomId = event.roomId ?: return false
        val room = roomGetter.getRoom(roomId) ?: return false
        val myDisplayName = room.membershipService().getRoomMember(userId)?.displayName ?: return false
        return condition.isSatisfied(event, myDisplayName)
    }
}
