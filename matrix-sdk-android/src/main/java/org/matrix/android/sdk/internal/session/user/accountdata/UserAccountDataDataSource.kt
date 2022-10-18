/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.user.accountdata

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.asLiveData
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmQuery
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataEvent
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.andIf
import org.matrix.android.sdk.internal.database.mapper.AccountDataMapper
import org.matrix.android.sdk.internal.database.model.UserAccountDataEntity
import org.matrix.android.sdk.internal.database.queryIn
import org.matrix.android.sdk.internal.di.SessionDatabase
import javax.inject.Inject

internal class UserAccountDataDataSource @Inject constructor(
        @SessionDatabase private val realmInstance: RealmInstance,
        private val accountDataMapper: AccountDataMapper
) {

    fun getAccountDataEvent(type: String): UserAccountDataEvent? {
        return getAccountDataEvents(setOf(type)).firstOrNull()
    }

    fun getLiveAccountDataEvent(type: String): LiveData<Optional<UserAccountDataEvent>> {
        return Transformations.map(getLiveAccountDataEvents(setOf(type))) {
            it.firstOrNull().toOptional()
        }
    }

    fun getAccountDataEvents(types: Set<String>): List<UserAccountDataEvent> {
        val realm = realmInstance.getBlockingRealm()
        return accountDataEventsQuery(realm, types).find().map(accountDataMapper::map)
    }

    fun getLiveAccountDataEvents(types: Set<String>): LiveData<List<UserAccountDataEvent>> {
        return realmInstance.queryList(accountDataMapper::map) {
            accountDataEventsQuery(it, types)
        }.asLiveData()
    }

    private fun accountDataEventsQuery(realm: TypedRealm, types: Set<String>): RealmQuery<UserAccountDataEntity> {
        return realm.query(UserAccountDataEntity::class)
                .andIf(types.isNotEmpty()) {
                    queryIn("type", types)
                }
    }
}
