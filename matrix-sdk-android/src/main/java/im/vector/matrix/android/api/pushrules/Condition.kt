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
package im.vector.matrix.android.api.pushrules

import im.vector.matrix.android.api.session.events.model.Event

abstract class Condition(val kind: Kind) {

    enum class Kind(val value: String) {
        EVENT_MATCH("event_match"),
        CONTAINS_DISPLAY_NAME("contains_display_name"),
        ROOM_MEMBER_COUNT("room_member_count"),
        SENDER_NOTIFICATION_PERMISSION("sender_notification_permission"),
        UNRECOGNIZE("");

        companion object {

            fun fromString(value: String): Kind {
                return when (value) {
                    "event_match"                    -> EVENT_MATCH
                    "contains_display_name"          -> CONTAINS_DISPLAY_NAME
                    "room_member_count"              -> ROOM_MEMBER_COUNT
                    "sender_notification_permission" -> SENDER_NOTIFICATION_PERMISSION
                    else                             -> UNRECOGNIZE
                }
            }

        }

    }

    abstract fun isSatisfied(event: Event): Boolean

    companion object {
        //TODO factory methods?
    }

}