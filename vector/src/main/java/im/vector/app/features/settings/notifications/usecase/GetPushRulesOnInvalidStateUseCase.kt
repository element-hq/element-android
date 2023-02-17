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
import im.vector.app.features.settings.notifications.getSyncedRules
import org.matrix.android.sdk.api.extensions.orTrue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.pushrules.RuleIds
import org.matrix.android.sdk.api.session.pushrules.getActions
import org.matrix.android.sdk.api.session.pushrules.rest.PushRule
import javax.inject.Inject

class GetPushRulesOnInvalidStateUseCase @Inject constructor() {

    fun execute(session: Session): List<PushRule> {
        val allRules = session.pushRuleService().getPushRules().getAllRules()
        return allRules.filter { it.isOnInvalidState(allRules) }
    }

    private fun PushRule.isOnInvalidState(allRules: List<PushRule>): Boolean {
        val parent = RuleIds.getParentRule(ruleId)?.let { parentId -> allRules.find { it.ruleId == parentId } }
        val children = RuleIds.getSyncedRules(ruleId).mapNotNull { childId -> allRules.find { it.ruleId == childId } }
        val isAlignedWithParent = parent?.let { isAlignedWithParentRule(it) }.orTrue()
        return !isAlignedWithParent || !isAlignedWithChildrenRules(children)
    }

    private fun PushRule.isAlignedWithParentRule(parent: PushRule) = this.getActions() == parent.getActions() && this.enabled == parent.enabled
    private fun PushRule.isAlignedWithChildrenRules(children: List<PushRule>) = children.all { it.isAlignedWithParentRule(this) }
}
