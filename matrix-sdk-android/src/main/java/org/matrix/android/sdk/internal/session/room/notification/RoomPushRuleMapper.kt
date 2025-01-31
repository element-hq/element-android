/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.notification

import org.matrix.android.sdk.api.session.pushrules.Action
import org.matrix.android.sdk.api.session.pushrules.Kind
import org.matrix.android.sdk.api.session.pushrules.RuleSetKey
import org.matrix.android.sdk.api.session.pushrules.getActions
import org.matrix.android.sdk.api.session.pushrules.rest.PushCondition
import org.matrix.android.sdk.api.session.pushrules.rest.PushRule
import org.matrix.android.sdk.api.session.pushrules.toJson
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import org.matrix.android.sdk.internal.database.mapper.PushRulesMapper
import org.matrix.android.sdk.internal.database.model.PushRuleEntity

internal fun PushRuleEntity.toRoomPushRule(): RoomPushRule? {
    val kind = parent?.firstOrNull()?.kind
    val pushRule = when (kind) {
        RuleSetKey.OVERRIDE -> {
            PushRulesMapper.map(this)
        }
        RuleSetKey.ROOM -> {
            PushRulesMapper.mapRoomRule(this)
        }
        else -> null
    }
    return if (pushRule == null || kind == null) {
        null
    } else {
        RoomPushRule(kind, pushRule)
    }
}

internal fun RoomNotificationState.toRoomPushRule(roomId: String): RoomPushRule? {
    return when {
        this == RoomNotificationState.ALL_MESSAGES -> null
        this == RoomNotificationState.ALL_MESSAGES_NOISY -> {
            val rule = PushRule(
                    actions = listOf(Action.Notify, Action.Sound()).toJson(),
                    enabled = true,
                    ruleId = roomId
            )
            return RoomPushRule(RuleSetKey.ROOM, rule)
        }
        else -> {
            val condition = PushCondition(
                    kind = Kind.EventMatch.value,
                    key = "room_id",
                    pattern = roomId
            )
            val rule = PushRule(
                    actions = listOf(Action.DoNotNotify).toJson(),
                    enabled = true,
                    ruleId = roomId,
                    conditions = listOf(condition)
            )
            val kind = if (this == RoomNotificationState.MUTE) {
                RuleSetKey.OVERRIDE
            } else {
                RuleSetKey.ROOM
            }
            return RoomPushRule(kind, rule)
        }
    }
}

internal fun RoomPushRule.toRoomNotificationState(): RoomNotificationState {
    return if (rule.enabled) {
        val actions = rule.getActions()
        if (actions.contains(Action.DoNotNotify)) {
            if (kind == RuleSetKey.OVERRIDE) {
                RoomNotificationState.MUTE
            } else {
                RoomNotificationState.MENTIONS_ONLY
            }
        } else if (actions.contains(Action.Notify)) {
            val hasSoundAction = actions.find {
                it is Action.Sound
            } != null
            if (hasSoundAction) {
                RoomNotificationState.ALL_MESSAGES_NOISY
            } else {
                RoomNotificationState.ALL_MESSAGES
            }
        } else {
            RoomNotificationState.ALL_MESSAGES
        }
    } else {
        RoomNotificationState.ALL_MESSAGES
    }
}
