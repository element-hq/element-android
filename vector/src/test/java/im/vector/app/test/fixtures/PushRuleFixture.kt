/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
