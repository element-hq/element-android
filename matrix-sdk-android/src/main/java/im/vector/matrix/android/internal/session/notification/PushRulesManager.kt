/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.internal.session.notification

import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.pushrules.PushRuleService
import im.vector.matrix.android.api.pushrules.PushRulesProvider
import im.vector.matrix.android.api.pushrules.domainActions
import im.vector.matrix.android.api.pushrules.rest.PushRule
import im.vector.matrix.android.api.session.events.model.Event


internal class PushRulesManager(
        private val sessionParams: SessionParams,
        private val pushRulesProvider: PushRulesProvider) : PushRuleService {


    private var listeners = ArrayList<PushRuleService.PushRuleListener>()


    override fun removePushRuleListener(listener: PushRuleService.PushRuleListener) {
        listeners.remove(listener)
    }


    override fun addPushRuleListener(listener: PushRuleService.PushRuleListener) {
        if (!listeners.contains(listener))
            listeners.add(listener)
    }

    fun processEvents(events: List<Event>) {
        var hasDoneSomething = false
        events.forEach { event ->
            fulfilledBingRule(event)?.let {
                hasDoneSomething = true
                dispatchBing(event, it)
            }
        }
        if (hasDoneSomething)
            dispatchFinish()
    }

    fun dispatchBing(event: Event, rule: PushRule) {
        try {
            listeners.forEach {
                it.onMatchRule(event, rule.domainActions() ?: emptyList())
            }
        } catch (e: Throwable) {

        }
    }

    fun dispatchFinish() {
        try {
            listeners.forEach {
                it.batchFinish()
            }
        } catch (e: Throwable) {

        }
    }

    fun fulfilledBingRule(event: Event): PushRule? {
        pushRulesProvider.getOrderedPushrules().forEach { rule ->
            rule.conditions?.mapNotNull { it.asExecutableCondition() }?.forEach {
                if (it.isSatisfied(event)) return rule
            }
        }
        return null
    }

}