/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.pushrules

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
