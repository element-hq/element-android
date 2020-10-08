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
package org.matrix.android.sdk.api.pushrules.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.pushrules.Condition
import org.matrix.android.sdk.api.pushrules.ContainsDisplayNameCondition
import org.matrix.android.sdk.api.pushrules.EventMatchCondition
import org.matrix.android.sdk.api.pushrules.Kind
import org.matrix.android.sdk.api.pushrules.RoomMemberCountCondition
import org.matrix.android.sdk.api.pushrules.RuleIds
import org.matrix.android.sdk.api.pushrules.SenderNotificationPermissionCondition
import timber.log.Timber

/**
 * Ref: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-pushrules
 */
@JsonClass(generateAdapter = true)
data class PushCondition(
        /**
         * Required. The kind of condition to apply.
         */
        @Json(name = "kind")
        val kind: String,

        /**
         * Required for event_match conditions. The dot- separated field of the event to match.
         */
        @Json(name = "key")
        val key: String? = null,

        /**
         * Required for event_match conditions.
         */
        @Json(name = "pattern")
        val pattern: String? = null,

        /**
         * Required for room_member_count conditions.
         * A decimal integer optionally prefixed by one of, ==, <, >, >= or <=.
         * A prefix of < matches rooms where the member count is strictly less than the given number and so forth.
         * If no prefix is present, this parameter defaults to ==.
         */
        @Json(name = "is")
        val iz: String? = null
) {

    fun asExecutableCondition(rule: PushRule): Condition? {
        return when (Kind.fromString(kind)) {
            Kind.EventMatch                   -> {
                if (key != null && pattern != null) {
                    EventMatchCondition(key, pattern, rule.ruleId == RuleIds.RULE_ID_CONTAIN_USER_NAME)
                } else {
                    Timber.e("Malformed Event match condition")
                    null
                }
            }
            Kind.ContainsDisplayName          -> {
                ContainsDisplayNameCondition()
            }
            Kind.RoomMemberCount              -> {
                if (iz.isNullOrEmpty()) {
                    Timber.e("Malformed ROOM_MEMBER_COUNT condition")
                    null
                } else {
                    RoomMemberCountCondition(iz)
                }
            }
            Kind.SenderNotificationPermission -> {
                if (key == null) {
                    Timber.e("Malformed Sender Notification Permission condition")
                    null
                } else {
                    SenderNotificationPermissionCondition(key)
                }
            }
            Kind.Unrecognised                 -> {
                Timber.e("Unknown kind $kind")
                null
            }
        }
    }
}
