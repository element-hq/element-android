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

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.pushrules.rest.PushRule
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.database.RealmLiveEntityObserver
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.types
import im.vector.matrix.android.internal.session.pushers.DefaultConditionResolver
import timber.log.Timber


internal class BingRuleWatcher(monarchy: Monarchy,
                               private val defaultPushRuleService: DefaultPushRuleService) :
        RealmLiveEntityObserver<EventEntity>(monarchy) {

    override val query = Monarchy.Query<EventEntity> {

        EventEntity.types(it, listOf(
                EventType.REDACTION, EventType.MESSAGE, EventType.REDACTION, EventType.ENCRYPTED)
        )

    }

    override fun processChanges(inserted: List<EventEntity>, updated: List<EventEntity>, deleted: List<EventEntity>) {
        //TODO task
        val rules = defaultPushRuleService.getPushrules("global")
        inserted.map { it.asDomain() }.let { events ->
            events.forEach { event ->
                fulfilledBingRule(event, rules)?.let {
                    Timber.v("Rule $it match for event ${event.eventId}")
                    defaultPushRuleService.dispatchBing(event, it)
                }
            }
        }
        defaultPushRuleService.dispatchFinish()
    }

    private fun fulfilledBingRule(event: Event, rules: List<PushRule>): PushRule? {
        val conditionResolver = DefaultConditionResolver(event)
        rules.filter { it.enabled }.forEach { rule ->
            val isFullfilled = rule.conditions?.map {
                it.asExecutableCondition()?.isSatisfied(conditionResolver) ?: false
            }?.fold(true/*A rule with no conditions always matches*/, { acc, next ->
                //All conditions must hold true for an event in order to apply the action for the event.
                acc && next
            }) ?: false

            if (isFullfilled) {
                return rule
            }
        }
        return null
    }


}