/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.matrix.android.internal.auth.db

import arrow.core.Try
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.api.auth.data.SessionParams
import io.realm.Realm
import io.realm.RealmConfiguration

internal class RealmSessionParamsStore(private val mapper: SessionParamsMapper,
                              private val realmConfiguration: RealmConfiguration) : SessionParamsStore {

    override fun save(sessionParams: SessionParams): Try<SessionParams> {
        return Try {
            val entity = mapper.map(sessionParams)
            if (entity != null) {
                val realm = Realm.getInstance(realmConfiguration)
                realm.executeTransaction {
                    it.insert(entity)
                }
                realm.close()
            }
            sessionParams
        }
    }

    override fun get(): SessionParams? {
        val realm = Realm.getInstance(realmConfiguration)
        val sessionParams = realm
                .where(SessionParamsEntity::class.java)
                .findAll()
                .map { mapper.map(it) }
                .lastOrNull()
        realm.close()
        return sessionParams
    }

}