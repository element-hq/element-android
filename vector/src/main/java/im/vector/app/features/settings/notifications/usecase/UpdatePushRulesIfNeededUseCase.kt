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

package im.vector.app.features.settings.notifications.usecase

import im.vector.app.features.settings.notifications.getParentRule
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.pushrules.RuleIds
import org.matrix.android.sdk.api.session.pushrules.getActions
import org.matrix.android.sdk.api.session.pushrules.rest.PushRule
import org.matrix.android.sdk.api.session.pushrules.rest.PushRuleAndKind
import javax.inject.Inject

class UpdatePushRulesIfNeededUseCase @Inject constructor() {

    suspend fun execute(session: Session) {
        val ruleSet = session.pushRuleService().getPushRules()
        val pushRules = ruleSet.getAllRules()
        val rulesToUpdate = pushRules.mapNotNull { rule ->
            val parent = RuleIds.getParentRule(rule.ruleId)?.let { ruleId -> ruleSet.findDefaultRule(ruleId) }
            if (parent != null && (rule.enabled != parent.pushRule.enabled || rule.actions != parent.pushRule.actions)) {
                PushRuleWithParent(rule, parent)
            } else {
                null
            }
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
