/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmSingleQuery
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.session.pushrules.RuleScope
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.model.PushRulesEntity
import org.matrix.android.sdk.internal.di.SessionDatabase
import javax.inject.Inject

internal class RoomPushRuleDataSource @Inject constructor(@SessionDatabase private val realmInstance: RealmInstance) {

    suspend fun getCurrentRoomPushRule(roomId: String): RoomPushRule? {
        val realm = realmInstance.getRealm()
        return queryPushRulesEntity(realm, roomId)
                .find()
                ?.toRoomPushRule(ruleId = roomId)
    }

    fun getLiveRoomNotificationState(roomId: String): LiveData<RoomNotificationState> {
        return realmInstance.queryFirst {
            queryPushRulesEntity(it, roomId)
        }.map {
            val pushRulesEntity = it.getOrNull()
            pushRulesEntity
                    ?.toRoomPushRule(ruleId = roomId)
                    ?.toRoomNotificationState()
                    ?: RoomNotificationState.ALL_MESSAGES
        }.asLiveData()
    }

    private fun queryPushRulesEntity(realm: TypedRealm, roomId: String): RealmSingleQuery<PushRulesEntity> {
        return realm.query(PushRulesEntity::class)
                .query("scope == $0", RuleScope.GLOBAL)
                .query("ANY pushRules.ruleId == $0", roomId)
                .first()
    }
}
