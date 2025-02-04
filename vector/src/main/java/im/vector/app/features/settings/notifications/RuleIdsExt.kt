/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.notifications

import org.matrix.android.sdk.api.session.pushrules.RuleIds

fun RuleIds.getSyncedRules(ruleId: String): List<String> {
    return when (ruleId) {
        RULE_ID_ONE_TO_ONE_ROOM -> listOf(
                RULE_ID_POLL_START_ONE_TO_ONE,
                RULE_ID_POLL_START_ONE_TO_ONE_UNSTABLE,
                RULE_ID_POLL_END_ONE_TO_ONE,
                RULE_ID_POLL_END_ONE_TO_ONE_UNSTABLE,
        )
        RULE_ID_ALL_OTHER_MESSAGES_ROOMS -> listOf(
                RULE_ID_POLL_START,
                RULE_ID_POLL_START_UNSTABLE,
                RULE_ID_POLL_END,
                RULE_ID_POLL_END_UNSTABLE,
        )
        else -> emptyList()
    }
}

fun RuleIds.getParentRule(ruleId: String): String? {
    return when (ruleId) {
        RULE_ID_POLL_START_ONE_TO_ONE,
        RULE_ID_POLL_START_ONE_TO_ONE_UNSTABLE,
        RULE_ID_POLL_END_ONE_TO_ONE,
        RULE_ID_POLL_END_ONE_TO_ONE_UNSTABLE -> RULE_ID_ONE_TO_ONE_ROOM
        RULE_ID_POLL_START,
        RULE_ID_POLL_START_UNSTABLE,
        RULE_ID_POLL_END,
        RULE_ID_POLL_END_UNSTABLE -> RULE_ID_ALL_OTHER_MESSAGES_ROOMS
        else -> null
    }
}
