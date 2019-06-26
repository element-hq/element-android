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
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.pushrules.Action
import im.vector.matrix.android.api.pushrules.PushRuleService
import im.vector.matrix.android.api.pushrules.rest.GetPushRulesResponse
import im.vector.matrix.android.api.pushrules.rest.PushRule
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.database.mapper.PushRulesMapper
import im.vector.matrix.android.internal.database.model.PushRulesEntity
import im.vector.matrix.android.internal.database.model.PusherEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.pushers.GetPushRulesTask
import im.vector.matrix.android.internal.session.pushers.UpdatePushRuleEnableStatusTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import timber.log.Timber


internal class DefaultPushRuleService(
        private val sessionParams: SessionParams,
        private val pushRulesTask: GetPushRulesTask,
        private val updatePushRuleEnableStatusTask: UpdatePushRuleEnableStatusTask,
        private val taskExecutor: TaskExecutor,
        private val monarchy: Monarchy
) : PushRuleService {


    private var listeners = ArrayList<PushRuleService.PushRuleListener>()


    override fun fetchPushRules(scope: String) {
        pushRulesTask
                .configureWith(Unit)
                .dispatchTo(object : MatrixCallback<GetPushRulesResponse> {
                    override fun onSuccess(data: GetPushRulesResponse) {
                        monarchy.runTransactionSync { realm ->
                            //clear existings?
                            //TODO
                            realm.where(PushRulesEntity::class.java)
                                    .equalTo(PusherEntityFields.USER_ID, sessionParams.credentials.userId)
                                    .findAll().deleteAllFromRealm()

                            val content = PushRulesEntity(sessionParams.credentials.userId, scope, "content")
                            data.global.content?.forEach { rule ->
                                PushRulesMapper.map(rule).also {
                                    content.pushRules.add(it)
                                }
                            }
                            realm.insertOrUpdate(content)

                            val override = PushRulesEntity(sessionParams.credentials.userId, scope, "override")
                            data.global.override?.forEach { rule ->
                                PushRulesMapper.map(rule).also {
                                    override.pushRules.add(it)
                                }
                            }
                            realm.insertOrUpdate(override)

                            val rooms = PushRulesEntity(sessionParams.credentials.userId, scope, "room")
                            data.global.room?.forEach { rule ->
                                PushRulesMapper.map(rule).also {
                                    rooms.pushRules.add(it)
                                }
                            }
                            realm.insertOrUpdate(rooms)

                            val senders = PushRulesEntity(sessionParams.credentials.userId, scope, "sender")
                            data.global.sender?.forEach { rule ->
                                PushRulesMapper.map(rule).also {
                                    senders.pushRules.add(it)
                                }
                            }
                            realm.insertOrUpdate(senders)

                            val underrides = PushRulesEntity(sessionParams.credentials.userId, scope, "underride")
                            data.global.underride?.forEach { rule ->
                                PushRulesMapper.map(rule).also {
                                    underrides.pushRules.add(it)
                                }
                            }
                            realm.insertOrUpdate(underrides)
                        }
                    }
                })
                .executeBy(taskExecutor)
    }

    override fun getPushRules(scope: String): List<PushRule> {

        var contentRules: List<PushRule> = emptyList()
        var overrideRules: List<PushRule> = emptyList()
        var roomRules: List<PushRule> = emptyList()
        var senderRules: List<PushRule> = emptyList()
        var underrideRules: List<PushRule> = emptyList()

        // TODO Create const for ruleSetKey
        monarchy.doWithRealm { realm ->
            PushRulesEntity.where(realm, sessionParams.credentials.userId, scope, "content").findFirst()?.let { re ->
                contentRules = re.pushRules.map { PushRulesMapper.mapContentRule(it) }
            }
            PushRulesEntity.where(realm, sessionParams.credentials.userId, scope, "override").findFirst()?.let { re ->
                overrideRules = re.pushRules.map { PushRulesMapper.map(it) }
            }
            PushRulesEntity.where(realm, sessionParams.credentials.userId, scope, "room").findFirst()?.let { re ->
                roomRules = re.pushRules.map { PushRulesMapper.mapRoomRule(it) }
            }
            PushRulesEntity.where(realm, sessionParams.credentials.userId, scope, "sender").findFirst()?.let { re ->
                senderRules = re.pushRules.map { PushRulesMapper.mapSenderRule(it) }
            }
            PushRulesEntity.where(realm, sessionParams.credentials.userId, scope, "underride").findFirst()?.let { re ->
                underrideRules = re.pushRules.map { PushRulesMapper.map(it) }
            }
        }

        return contentRules + overrideRules + roomRules + senderRules + underrideRules
    }

    override fun updatePushRuleEnableStatus(kind: String, pushRule: PushRule, enabled: Boolean, callback: MatrixCallback<Unit>) {
        updatePushRuleEnableStatusTask
                .configureWith(UpdatePushRuleEnableStatusTask.Params(kind, pushRule, enabled))
                // TODO Fetch the rules
                .dispatchTo(callback)
                .executeBy(taskExecutor)
    }

    override fun removePushRuleListener(listener: PushRuleService.PushRuleListener) {
        listeners.remove(listener)
    }


    override fun addPushRuleListener(listener: PushRuleService.PushRuleListener) {
        if (!listeners.contains(listener))
            listeners.add(listener)
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
        try {
            listeners.forEach {
                it.onMatchRule(event, Action.mapFrom(rule) ?: emptyList())
            }
        } catch (e: Throwable) {
            Timber.e(e, "Error while dispatching bing")
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
}