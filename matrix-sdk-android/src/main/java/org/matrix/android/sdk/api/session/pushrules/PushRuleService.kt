/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.pushrules

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.pushrules.rest.PushRule
import org.matrix.android.sdk.api.session.pushrules.rest.RuleSet

interface PushRuleService {
    /**
     * Fetch the push rules from the server.
     */
    fun fetchPushRules(scope: String = RuleScope.GLOBAL)

    fun getPushRules(scope: String = RuleScope.GLOBAL): RuleSet

    suspend fun updatePushRuleEnableStatus(kind: RuleKind, pushRule: PushRule, enabled: Boolean)

    suspend fun addPushRule(kind: RuleKind, pushRule: PushRule)

    /**
     * Enables/Disables a push rule and updates the actions if necessary.
     * @param kind the rule kind
     * @param ruleId the rule id
     * @param enable Enables/Disables the rule
     * @param actions Actions to update if not null
     */
    suspend fun updatePushRuleActions(kind: RuleKind, ruleId: String, enable: Boolean, actions: List<Action>?)

    suspend fun removePushRule(kind: RuleKind, ruleId: String)

    fun addPushRuleListener(listener: PushRuleListener)

    fun removePushRuleListener(listener: PushRuleListener)

    fun getActions(event: Event): List<Action>

//    fun fulfilledBingRule(event: Event, rules: List<PushRule>): PushRule?

    fun resolveSenderNotificationPermissionCondition(
            event: Event,
            condition: SenderNotificationPermissionCondition
    ): Boolean

    interface PushRuleListener {
        fun onEvents(pushEvents: PushEvents)
    }

    fun getKeywords(): LiveData<Set<String>>
}
