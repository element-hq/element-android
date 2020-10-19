/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.notification

import org.matrix.android.sdk.api.pushrules.Action
import org.matrix.android.sdk.api.pushrules.Kind
import org.matrix.android.sdk.api.pushrules.RuleSetKey
import org.matrix.android.sdk.api.pushrules.getActions
import org.matrix.android.sdk.api.pushrules.rest.PushCondition
import org.matrix.android.sdk.api.pushrules.rest.PushRule
import org.matrix.android.sdk.api.pushrules.toJson
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import org.matrix.android.sdk.internal.database.mapper.PushRulesMapper
import org.matrix.android.sdk.internal.database.model.PushRuleEntity

internal fun PushRuleEntity.toRoomPushRule(): RoomPushRule? {
    val kind = parent?.firstOrNull()?.kind
    val pushRule = when (kind) {
        RuleSetKey.OVERRIDE -> {
            PushRulesMapper.map(this)
        }
        RuleSetKey.ROOM     -> {
            PushRulesMapper.mapRoomRule(this)
        }
        else                -> null
    }
    return if (pushRule == null || kind == null) {
        null
    } else {
        RoomPushRule(kind, pushRule)
    }
}

internal fun RoomNotificationState.toRoomPushRule(roomId: String): RoomPushRule? {
    return when {
        this == RoomNotificationState.ALL_MESSAGES       -> null
        this == RoomNotificationState.ALL_MESSAGES_NOISY -> {
            val rule = PushRule(
                    actions = listOf(Action.Notify, Action.Sound()).toJson(),
                    enabled = true,
                    ruleId = roomId
            )
            return RoomPushRule(RuleSetKey.ROOM, rule)
        }
        else                                             -> {
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
