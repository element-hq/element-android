/*
 * Copyright (c) 2023 New Vector Ltd
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
