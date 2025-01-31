/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.pushrules

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.pushrules.ConditionResolver
import org.matrix.android.sdk.api.session.pushrules.rest.PushRule
import javax.inject.Inject

internal class PushRuleFinder @Inject constructor(
        private val conditionResolver: ConditionResolver
) {
    fun fulfilledBingRule(event: Event, rules: List<PushRule>): PushRule? {
        return rules.firstOrNull { rule ->
            // All conditions must hold true for an event in order to apply the action for the event.
            rule.enabled && rule.conditions?.all {
                it.asExecutableCondition(rule)?.isSatisfied(event, conditionResolver) ?: false
            } ?: false
        }
    }
}
