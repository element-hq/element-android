/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.pushrules.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.pushrules.Condition
import org.matrix.android.sdk.api.session.pushrules.ContainsDisplayNameCondition
import org.matrix.android.sdk.api.session.pushrules.EventMatchCondition
import org.matrix.android.sdk.api.session.pushrules.Kind
import org.matrix.android.sdk.api.session.pushrules.RoomMemberCountCondition
import org.matrix.android.sdk.api.session.pushrules.RuleIds
import org.matrix.android.sdk.api.session.pushrules.SenderNotificationPermissionCondition
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
            Kind.EventMatch -> {
                if (key != null && pattern != null) {
                    EventMatchCondition(key, pattern, rule.ruleId == RuleIds.RULE_ID_CONTAIN_USER_NAME)
                } else {
                    Timber.e("Malformed Event match condition")
                    null
                }
            }
            Kind.ContainsDisplayName -> {
                ContainsDisplayNameCondition()
            }
            Kind.RoomMemberCount -> {
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
            Kind.Unrecognised -> {
                Timber.e("Unknown kind $kind")
                null
            }
        }
    }
}
