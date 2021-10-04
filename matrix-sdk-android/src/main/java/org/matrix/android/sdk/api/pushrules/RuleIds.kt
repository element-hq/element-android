/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.pushrules

/**
 * Known rule ids
 *
 * Ref: https://matrix.org/docs/spec/client_server/latest#predefined-rules
 */
object RuleIds {
    // Default Override Rules
    const val RULE_ID_DISABLE_ALL = ".m.rule.master"
    const val RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS = ".m.rule.suppress_notices"
    const val RULE_ID_INVITE_ME = ".m.rule.invite_for_me"
    const val RULE_ID_PEOPLE_JOIN_LEAVE = ".m.rule.member_event"
    const val RULE_ID_CONTAIN_DISPLAY_NAME = ".m.rule.contains_display_name"

    const val RULE_ID_TOMBSTONE = ".m.rule.tombstone"
    const val RULE_ID_ROOM_NOTIF = ".m.rule.roomnotif"

    // Default Content Rules
    const val RULE_ID_CONTAIN_USER_NAME = ".m.rule.contains_user_name"

    // The keywords rule id is not a "real" id in that it does not exist server-side.
    // It is used client-side as a placeholder for rendering the keyword push rule setting
    // alongside the others. A similar approach and naming is used on Web and iOS.
    const val RULE_ID_KEYWORDS = "_keywords"

    // Default Underride Rules
    const val RULE_ID_CALL = ".m.rule.call"
    const val RULE_ID_ONE_TO_ONE_ENCRYPTED_ROOM = ".m.rule.encrypted_room_one_to_one"
    const val RULE_ID_ONE_TO_ONE_ROOM = ".m.rule.room_one_to_one"
    const val RULE_ID_ALL_OTHER_MESSAGES_ROOMS = ".m.rule.message"
    const val RULE_ID_ENCRYPTED = ".m.rule.encrypted"

    // Not documented
    const val RULE_ID_FALLBACK = ".m.rule.fallback"

    const val RULE_ID_REACTION = ".m.rule.reaction"
}
