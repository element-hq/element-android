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

package im.vector.matrix.android.internal.auth.db

import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.database.awaitTransaction
import im.vector.matrix.android.internal.di.AuthDatabase
import io.realm.Realm
import io.realm.RealmConfiguration
import javax.inject.Inject

internal class RealmSessionParamsStore @Inject constructor(private val mapper: SessionParamsMapper,
                                                           @AuthDatabase
                                                           private val realmConfiguration: RealmConfiguration
) : SessionParamsStore {

    override fun getLast(): SessionParams? {
        val realm = Realm.getInstance(realmConfiguration)
        val sessionParams = realm
                .where(SessionParamsEntity::class.java)
                .findAll()
                .map { mapper.map(it) }
                .lastOrNull()
        realm.close()
        return sessionParams
    }

    override fun get(userId: String): SessionParams? {
        val realm = Realm.getInstance(realmConfiguration)
        val sessionParams = realm
                .where(SessionParamsEntity::class.java)
                .equalTo(SessionParamsEntityFields.USER_ID, userId)
                .findAll()
                .map { mapper.map(it) }
                .firstOrNull()
        realm.close()
        return sessionParams
    }

    override fun getAll(): List<SessionParams> {
        val realm = Realm.getInstance(realmConfiguration)
        val sessionParams = realm
                .where(SessionParamsEntity::class.java)
                .findAll()
                .mapNotNull { mapper.map(it) }
        realm.close()
        return sessionParams
    }

    override suspend fun save(sessionParams: SessionParams) {
        awaitTransaction(realmConfiguration) {
            val entity = mapper.map(sessionParams)
            if (entity != null) {
                it.insert(entity)
            }
        }
    }

    override suspend fun delete(userId: String) {
        awaitTransaction(realmConfiguration) {
            it.where(SessionParamsEntity::class.java)
                    .equalTo(SessionParamsEntityFields.USER_ID, userId)
                    .findAll()
                    .deleteAllFromRealm()
        }
    }

    override suspend fun deleteAll() {
        awaitTransaction(realmConfiguration) {
            it.where(SessionParamsEntity::class.java)
                    .findAll()
                    .deleteAllFromRealm()
        }
    }
}
