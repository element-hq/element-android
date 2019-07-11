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

abstract class Condition(val kind: Kind) {

    enum class Kind(val value: String) {
        event_match("event_match"),
        contains_display_name("contains_display_name"),
        room_member_count("room_member_count"),
        sender_notification_permission("sender_notification_permission"),
        UNRECOGNIZE("");

        companion object {

            fun fromString(value: String): Kind {
                return when (value) {
                    "event_match"                    -> event_match
                    "contains_display_name"          -> contains_display_name
                    "room_member_count"              -> room_member_count
                    "sender_notification_permission" -> sender_notification_permission
                    else                             -> UNRECOGNIZE
                }
            }

        }

    }

    abstract fun isSatisfied(conditionResolver: ConditionResolver): Boolean

    open fun technicalDescription(): String {
        return "Kind: $kind"
    }
}