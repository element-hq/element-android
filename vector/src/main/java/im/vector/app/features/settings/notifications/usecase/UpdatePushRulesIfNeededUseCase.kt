/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.notifications.usecase

import im.vector.app.features.settings.notifications.getParentRule
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.pushrules.RuleIds
import org.matrix.android.sdk.api.session.pushrules.getActions
import org.matrix.android.sdk.api.session.pushrules.rest.PushRule
import org.matrix.android.sdk.api.session.pushrules.rest.PushRuleAndKind
import javax.inject.Inject

class UpdatePushRulesIfNeededUseCase @Inject constructor(
        private val getPushRulesOnInvalidStateUseCase: GetPushRulesOnInvalidStateUseCase,
) {

    suspend fun execute(session: Session) {
        val rulesOnError = getPushRulesOnInvalidStateUseCase.execute(session).takeUnless { it.isEmpty() } ?: return

        val ruleSet = session.pushRuleService().getPushRules()
        val rulesToUpdate = rulesOnError.mapNotNull { rule ->
            RuleIds.getParentRule(rule.ruleId)
                    ?.let { ruleId -> ruleSet.findDefaultRule(ruleId) }
                    ?.let { PushRuleWithParent(rule, it) }
        }

        rulesToUpdate.forEach {
            session.pushRuleService().updatePushRuleActions(
                    kind = it.parent.kind,
                    ruleId = it.rule.ruleId,
                    enable = it.parent.pushRule.enabled,
                    actions = it.parent.pushRule.getActions(),
            )
        }
    }

    private data class PushRuleWithParent(
            val rule: PushRule,
            val parent: PushRuleAndKind,
    )
}
