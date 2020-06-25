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
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.pushrules.PushRuleService
import im.vector.matrix.android.api.pushrules.RuleKind
import im.vector.matrix.android.api.pushrules.RuleSetKey
import im.vector.matrix.android.api.pushrules.getActions
import im.vector.matrix.android.api.pushrules.rest.PushRule
import im.vector.matrix.android.api.pushrules.rest.RuleSet
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.mapper.PushRulesMapper
import im.vector.matrix.android.internal.database.model.PushRulesEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.session.pushers.AddPushRuleTask
import im.vector.matrix.android.internal.session.pushers.GetPushRulesTask
import im.vector.matrix.android.internal.session.pushers.RemovePushRuleTask
import im.vector.matrix.android.internal.session.pushers.UpdatePushRuleActionsTask
import im.vector.matrix.android.internal.session.pushers.UpdatePushRuleEnableStatusTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class DefaultPushRuleService @Inject constructor(
        private val getPushRulesTask: GetPushRulesTask,
        private val updatePushRuleEnableStatusTask: UpdatePushRuleEnableStatusTask,
        private val addPushRuleTask: AddPushRuleTask,
        private val updatePushRuleActionsTask: UpdatePushRuleActionsTask,
        private val removePushRuleTask: RemovePushRuleTask,
        private val taskExecutor: TaskExecutor,
        @SessionDatabase private val monarchy: Monarchy
) : PushRuleService {

    private var listeners = mutableSetOf<PushRuleService.PushRuleListener>()

    override fun fetchPushRules(scope: String) {
        getPushRulesTask
                .configureWith(GetPushRulesTask.Params(scope))
                .executeBy(taskExecutor)
    }

    override fun getPushRules(scope: String): RuleSet {
        var contentRules: List<PushRule> = emptyList()
        var overrideRules: List<PushRule> = emptyList()
        var roomRules: List<PushRule> = emptyList()
        var senderRules: List<PushRule> = emptyList()
        var underrideRules: List<PushRule> = emptyList()

        monarchy.doWithRealm { realm ->
            PushRulesEntity.where(realm, scope, RuleSetKey.CONTENT)
                    .findFirst()
                    ?.let { pushRulesEntity ->
                        contentRules = pushRulesEntity.pushRules.map { PushRulesMapper.mapContentRule(it) }
                    }
            PushRulesEntity.where(realm, scope, RuleSetKey.OVERRIDE)
                    .findFirst()
                    ?.let { pushRulesEntity ->
                        overrideRules = pushRulesEntity.pushRules.map { PushRulesMapper.map(it) }
                    }
            PushRulesEntity.where(realm, scope, RuleSetKey.ROOM)
                    .findFirst()
                    ?.let { pushRulesEntity ->
                        roomRules = pushRulesEntity.pushRules.map { PushRulesMapper.mapRoomRule(it) }
                    }
            PushRulesEntity.where(realm, scope, RuleSetKey.SENDER)
                    .findFirst()
                    ?.let { pushRulesEntity ->
                        senderRules = pushRulesEntity.pushRules.map { PushRulesMapper.mapSenderRule(it) }
                    }
            PushRulesEntity.where(realm, scope, RuleSetKey.UNDERRIDE)
                    .findFirst()
                    ?.let { pushRulesEntity ->
                        underrideRules = pushRulesEntity.pushRules.map { PushRulesMapper.map(it) }
                    }
        }

        return RuleSet(
                content = contentRules,
                override = overrideRules,
                room = roomRules,
                sender = senderRules,
                underride = underrideRules
        )
    }

    override fun updatePushRuleEnableStatus(kind: RuleKind, pushRule: PushRule, enabled: Boolean, callback: MatrixCallback<Unit>): Cancelable {
        // The rules will be updated, and will come back from the next sync response
        return updatePushRuleEnableStatusTask
                .configureWith(UpdatePushRuleEnableStatusTask.Params(kind, pushRule, enabled)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun addPushRule(kind: RuleKind, pushRule: PushRule, callback: MatrixCallback<Unit>): Cancelable {
        return addPushRuleTask
                .configureWith(AddPushRuleTask.Params(kind, pushRule)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun updatePushRuleActions(kind: RuleKind, oldPushRule: PushRule, newPushRule: PushRule, callback: MatrixCallback<Unit>): Cancelable {
        return updatePushRuleActionsTask
                .configureWith(UpdatePushRuleActionsTask.Params(kind, oldPushRule, newPushRule)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun removePushRule(kind: RuleKind, pushRule: PushRule, callback: MatrixCallback<Unit>): Cancelable {
        return removePushRuleTask
                .configureWith(RemovePushRuleTask.Params(kind, pushRule)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun removePushRuleListener(listener: PushRuleService.PushRuleListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    override fun addPushRuleListener(listener: PushRuleService.PushRuleListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

//    fun processEvents(events: List<Event>) {
//        var hasDoneSomething = false
//        events.forEach { event ->
//            fulfilledBingRule(event)?.let {
//                hasDoneSomething = true
//                dispatchBing(event, it)
//            }
//        }
//        if (hasDoneSomething)
//            dispatchFinish()
//    }

    fun dispatchBing(event: Event, rule: PushRule) {
        synchronized(listeners) {
            val actionsList = rule.getActions()
            listeners.forEach {
                try {
                    it.onMatchRule(event, actionsList)
                } catch (e: Throwable) {
                    Timber.e(e, "Error while dispatching bing")
                }
            }
        }
    }

    fun dispatchRoomJoined(roomId: String) {
        synchronized(listeners) {
            listeners.forEach {
                try {
                    it.onRoomJoined(roomId)
                } catch (e: Throwable) {
                    Timber.e(e, "Error while dispatching room joined")
                }
            }
        }
    }

    fun dispatchRoomLeft(roomId: String) {
        synchronized(listeners) {
            listeners.forEach {
                try {
                    it.onRoomLeft(roomId)
                } catch (e: Throwable) {
                    Timber.e(e, "Error while dispatching room left")
                }
            }
        }
    }

    fun dispatchRedactedEventId(redactedEventId: String) {
        synchronized(listeners) {
            listeners.forEach {
                try {
                    it.onEventRedacted(redactedEventId)
                } catch (e: Throwable) {
                    Timber.e(e, "Error while dispatching redacted event")
                }
            }
        }
    }

    fun dispatchFinish() {
        synchronized(listeners) {
            listeners.forEach {
                try {
                    it.batchFinish()
                } catch (e: Throwable) {
                    Timber.e(e, "Error while dispatching finish")
                }
            }
        }
    }
}
