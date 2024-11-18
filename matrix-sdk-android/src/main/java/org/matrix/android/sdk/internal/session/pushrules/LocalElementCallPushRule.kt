/*
 * Copyright (c) 2024 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.pushrules

import org.matrix.android.sdk.api.session.pushrules.Action
import org.matrix.android.sdk.api.session.pushrules.RuleIds
import org.matrix.android.sdk.api.session.pushrules.rest.PushCondition
import org.matrix.android.sdk.api.session.pushrules.rest.PushRule

private val localElementCallPushRule = PushRule(
        ruleId = RuleIds.RULE_ID_ELEMENT_CALL_NOTIFY,
        conditions = listOf(
                PushCondition(
                        kind = "event_match",
                        key = "type",
                        pattern = "m.call.notify",
                )
        ),
        actions = listOf(
                Action.ACTION_NOTIFY,
        ),
        enabled = true,
)

private val localElementCallPushRuleUnstable = PushRule(
        ruleId = RuleIds.RULE_ID_ELEMENT_CALL_NOTIFY_UNSTABLE,
        conditions = listOf(
                PushCondition(
                        kind = "event_match",
                        key = "type",
                        pattern = "org.matrix.msc4075.call.notify",
                )
        ),
        actions = listOf(
                Action.ACTION_NOTIFY,
        ),
        enabled = true,
)

/**
 * Ensure that the element call push rules are present.
 */
fun List<PushRule>.withElementCallPushRules(): List<PushRule> {
    val ruleIds = map { it.ruleId }
    return buildList {
        addAll(this@withElementCallPushRules)
        if (!ruleIds.contains(localElementCallPushRule.ruleId)) {
            add(localElementCallPushRule)
        }
        if (!ruleIds.contains(localElementCallPushRuleUnstable.ruleId)) {
            add(localElementCallPushRuleUnstable)
        }
    }
}
