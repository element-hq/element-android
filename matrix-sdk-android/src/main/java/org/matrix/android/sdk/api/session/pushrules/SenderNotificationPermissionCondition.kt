/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.pushrules

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper

class SenderNotificationPermissionCondition(
        /**
         * A string that determines the power level the sender must have to trigger notifications of a given type,
         * such as room. Refer to the m.room.power_levels event schema for information about what the defaults are
         * and how to interpret the event. The key is used to look up the power level required to send a notification
         * type from the notifications object in the power level event content.
         */
        val key: String
) : Condition {

    override fun isSatisfied(event: Event, conditionResolver: ConditionResolver): Boolean {
        return conditionResolver.resolveSenderNotificationPermissionCondition(event, this)
    }

    override fun technicalDescription() = "User power level <$key>"

    fun isSatisfied(event: Event, powerLevels: PowerLevelsContent): Boolean {
        val powerLevelsHelper = PowerLevelsHelper(powerLevels)
        return event.senderId != null && powerLevelsHelper.getUserPowerLevelValue(event.senderId) >= powerLevels.notificationLevel(key)
    }
}
