/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.android.sdk.api.pushrules

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.pushrules.rest.PushRule
import org.matrix.android.sdk.api.pushrules.rest.RuleSet
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.util.Cancelable

interface PushRuleService {
    /**
     * Fetch the push rules from the server
     */
    fun fetchPushRules(scope: String = RuleScope.GLOBAL)

    fun getPushRules(scope: String = RuleScope.GLOBAL): RuleSet

    fun updatePushRuleEnableStatus(kind: RuleKind, pushRule: PushRule, enabled: Boolean, callback: MatrixCallback<Unit>): Cancelable

    fun addPushRule(kind: RuleKind, pushRule: PushRule, callback: MatrixCallback<Unit>): Cancelable

    fun updatePushRuleActions(kind: RuleKind, oldPushRule: PushRule, newPushRule: PushRule, callback: MatrixCallback<Unit>): Cancelable

    fun removePushRule(kind: RuleKind, pushRule: PushRule, callback: MatrixCallback<Unit>): Cancelable

    fun addPushRuleListener(listener: PushRuleListener)

    fun removePushRuleListener(listener: PushRuleListener)

//    fun fulfilledBingRule(event: Event, rules: List<PushRule>): PushRule?

    interface PushRuleListener {
        fun onMatchRule(event: Event, actions: List<Action>)
        fun onRoomJoined(roomId: String)
        fun onRoomLeft(roomId: String)
        fun onEventRedacted(redactedEventId: String)
        fun batchFinish()
    }
}
