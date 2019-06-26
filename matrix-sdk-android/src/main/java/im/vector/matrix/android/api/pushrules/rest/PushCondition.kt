/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.api.pushrules.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.pushrules.*
import timber.log.Timber

@JsonClass(generateAdapter = true)
data class PushCondition(
        /**
         * Required. The kind of condition to apply.
         */
        val kind: String,

        /**
         * Required for event_match conditions. The dot- separated field of the event to match.
         */

        val key: String? = null,
        /**
         *Required for event_match conditions.
         */

        val pattern: String? = null,
        /**
         * Required for room_member_count conditions.
         * A decimal integer optionally prefixed by one of, ==, <, >, >= or <=.
         * A prefix of < matches rooms where the member count is strictly less than the given number and so forth. If no prefix is present, this parameter defaults to ==.
         */
        @Json(name = "is") val iz: String? = null
) {

    fun asExecutableCondition(): Condition? {
        return when (Condition.Kind.fromString(this.kind)) {
            Condition.Kind.event_match                    -> {
                if (this.key != null && this.pattern != null) {
                    EventMatchCondition(key, pattern)
                } else {
                    Timber.e("Malformed Event match condition")
                    null
                }
            }
            Condition.Kind.contains_display_name          -> {
                ContainsDisplayNameCondition()
            }
            Condition.Kind.room_member_count              -> {
                if (this.iz.isNullOrBlank()) {
                    Timber.e("Malformed ROOM_MEMBER_COUNT condition")
                    null
                } else {
                    RoomMemberCountCondition(this.iz)
                }
            }
            Condition.Kind.sender_notification_permission -> {
                this.key?.let { SenderNotificationPermissionCondition(it) }
            }
            Condition.Kind.UNRECOGNIZE                    -> {
                Timber.e("Unknwon kind $kind")
                null
            }
        }
    }
}