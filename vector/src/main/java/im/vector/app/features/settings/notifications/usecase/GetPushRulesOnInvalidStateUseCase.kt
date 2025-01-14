/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
