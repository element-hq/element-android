/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.notification

import org.matrix.android.sdk.api.pushrules.ConditionResolver
import org.matrix.android.sdk.api.pushrules.rest.PushRule
import org.matrix.android.sdk.api.session.events.model.Event
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
