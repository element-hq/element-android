/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.user.accountdata

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.api.util.toOptional
import im.vector.matrix.android.internal.database.mapper.AccountDataMapper
import im.vector.matrix.android.internal.database.model.UserAccountDataEntity
import im.vector.matrix.android.internal.database.model.UserAccountDataEntityFields
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.api.session.accountdata.UserAccountDataEvent
import io.realm.Realm
import io.realm.RealmQuery
import javax.inject.Inject

internal class AccountDataDataSource @Inject constructor(@SessionDatabase private val monarchy: Monarchy,
                                                         private val accountDataMapper: AccountDataMapper) {

    fun getAccountDataEvent(type: String): UserAccountDataEvent? {
        return getAccountDataEvents(setOf(type)).firstOrNull()
    }

    fun getLiveAccountDataEvent(type: String): LiveData<Optional<UserAccountDataEvent>> {
        return Transformations.map(getLiveAccountDataEvents(setOf(type))) {
            it.firstOrNull()?.toOptional()
        }
    }

    fun getAccountDataEvents(types: Set<String>): List<UserAccountDataEvent> {
        return monarchy.fetchAllMappedSync(
                { accountDataEventsQuery(it, types) },
                accountDataMapper::map
        )
    }

    fun getLiveAccountDataEvents(types: Set<String>): LiveData<List<UserAccountDataEvent>> {
        return monarchy.findAllMappedWithChanges(
                { accountDataEventsQuery(it, types) },
                accountDataMapper::map
        )
    }

    private fun accountDataEventsQuery(realm: Realm, types: Set<String>): RealmQuery<UserAccountDataEntity> {
        val query = realm.where(UserAccountDataEntity::class.java)
        if (types.isNotEmpty()) {
            query.`in`(UserAccountDataEntityFields.TYPE, types.toTypedArray())
        }
        return query
    }
}
