/*
 * Copyright 2019 New Vector Ltd
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

package org.matrix.android.sdk.api.pushrules.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.pushrules.Action
import org.matrix.android.sdk.api.pushrules.getActions
import org.matrix.android.sdk.api.pushrules.toJson

/**
 * Ref: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-pushrules
 */
@JsonClass(generateAdapter = true)
data class PushRule(
        /**
         * Required. The actions to perform when this rule is matched.
         */
        @Json(name = "actions")
        val actions: List<Any>,
        /**
         * Required. Whether this is a default rule, or has been set explicitly.
         */
        @Json(name = "default")
        val default: Boolean? = false,
        /**
         * Required. Whether the push rule is enabled or not.
         */
        @Json(name = "enabled")
        val enabled: Boolean,
        /**
         * Required. The ID of this rule.
         */
        @Json(name = "rule_id")
        val ruleId: String,
        /**
         * The conditions that must hold true for an event in order for a rule to be applied to an event
         */
        @Json(name = "conditions")
        val conditions: List<PushCondition>? = null,
        /**
         * The glob-style pattern to match against. Only applicable to content rules.
         */
        @Json(name = "pattern")
        val pattern: String? = null
) {
    /**
     * Add the default notification sound.
     */
    fun setNotificationSound(): PushRule {
        return setNotificationSound(ACTION_VALUE_DEFAULT)
    }

    fun getNotificationSound(): String? {
        return (getActions().firstOrNull { it is Action.Sound } as? Action.Sound)?.sound
    }

    /**
     * Set the notification sound
     *
     * @param sound notification sound
     */
    fun setNotificationSound(sound: String): PushRule {
        return copy(
                actions = (getActions().filter { it !is Action.Sound } + Action.Sound(sound)).toJson()
        )
    }

    /**
     * Remove the notification sound
     */
    fun removeNotificationSound(): PushRule {
        return copy(
                actions = getActions().filter { it !is Action.Sound }.toJson()
        )
    }

    /**
     * Set the highlight status.
     *
     * @param highlight the highlight status
     */
    fun setHighlight(highlight: Boolean): PushRule {
        return copy(
                actions = (getActions().filter { it !is Action.Highlight } + Action.Highlight(highlight)).toJson()
        )
    }

    /**
     * Set the notification status.
     *
     * @param notify true to notify
     */
    fun setNotify(notify: Boolean): PushRule {
        val mutableActions = actions.toMutableList()

        mutableActions.remove(ACTION_DONT_NOTIFY)
        mutableActions.remove(ACTION_NOTIFY)

        if (notify) {
            mutableActions.add(ACTION_NOTIFY)
        } else {
            mutableActions.add(ACTION_DONT_NOTIFY)
        }

        return copy(actions = mutableActions)
    }

    /**
     * Return true if the rule should highlight the event.
     *
     * @return true if the rule should play sound
     */
    fun shouldNotify() = actions.contains(ACTION_NOTIFY)

    /**
     * Return true if the rule should not highlight the event.
     *
     * @return true if the rule should not play sound
     */
    fun shouldNotNotify() = actions.contains(ACTION_DONT_NOTIFY)

    companion object {
        /* ==========================================================================================
         * Rule id
         * ========================================================================================== */

        const val RULE_ID_DISABLE_ALL = ".m.rule.master"
        const val RULE_ID_CONTAIN_USER_NAME = ".m.rule.contains_user_name"
        const val RULE_ID_CONTAIN_DISPLAY_NAME = ".m.rule.contains_display_name"
        const val RULE_ID_ONE_TO_ONE_ROOM = ".m.rule.room_one_to_one"
        const val RULE_ID_INVITE_ME = ".m.rule.invite_for_me"
        const val RULE_ID_PEOPLE_JOIN_LEAVE = ".m.rule.member_event"
        const val RULE_ID_CALL = ".m.rule.call"
        const val RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS = ".m.rule.suppress_notices"
        const val RULE_ID_ALL_OTHER_MESSAGES_ROOMS = ".m.rule.message"
        const val RULE_ID_AT_ROOMS = ".m.rule.roomnotif"
        const val RULE_ID_TOMBSTONE = ".m.rule.tombstone"
        const val RULE_ID_E2E_ONE_TO_ONE_ROOM = ".m.rule.encrypted_room_one_to_one"
        const val RULE_ID_E2E_GROUP = ".m.rule.encrypted"
        const val RULE_ID_REACTION = ".m.rule.reaction"
        const val RULE_ID_FALLBACK = ".m.rule.fallback"

        /* ==========================================================================================
         * Actions
         * ========================================================================================== */

        const val ACTION_NOTIFY = "notify"
        const val ACTION_DONT_NOTIFY = "dont_notify"
        const val ACTION_COALESCE = "coalesce"

        const val ACTION_SET_TWEAK_SOUND_VALUE = "sound"
        const val ACTION_SET_TWEAK_HIGHLIGHT_VALUE = "highlight"

        const val ACTION_PARAMETER_SET_TWEAK = "set_tweak"
        const val ACTION_PARAMETER_VALUE = "value"

        const val ACTION_VALUE_DEFAULT = "default"
        const val ACTION_VALUE_RING = "ring"
    }
}
