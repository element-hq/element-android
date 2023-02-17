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

package im.vector.app.test.fixtures

import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.session.pushrules.Action
import org.matrix.android.sdk.api.session.pushrules.RuleSetKey
import org.matrix.android.sdk.api.session.pushrules.getActions
import org.matrix.android.sdk.api.session.pushrules.rest.PushRule
import org.matrix.android.sdk.api.session.pushrules.rest.PushRuleAndKind

object PushRuleFixture {

    private const val A_RULE_ID = "a-rule-id"

    // Needs: mockkStatic("org.matrix.android.sdk.api.session.pushrules.ActionKt")
    fun aPushRule(
            ruleId: String = A_RULE_ID,
            enabled: Boolean = true,
            actions: List<Action> = listOf(Action.Notify),
    ): PushRule = mockk {
        every { this@mockk.ruleId } returns ruleId
        every { this@mockk.enabled } returns enabled
        every { getActions() } returns actions
    }

    fun aPushRuleAndKind(
            pushRule: PushRule = aPushRule(),
            kind: RuleSetKey = RuleSetKey.UNDERRIDE,
    ): PushRuleAndKind = mockk {
        every { this@mockk.pushRule } returns pushRule
        every { this@mockk.kind } returns kind
    }
}
