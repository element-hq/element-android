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

import im.vector.matrix.android.internal.auth.PendingSessionStore
import im.vector.matrix.android.internal.database.awaitTransaction
import im.vector.matrix.android.internal.di.AuthDatabase
import io.realm.Realm
import io.realm.RealmConfiguration
import javax.inject.Inject

internal class RealmPendingSessionStore @Inject constructor(private val mapper: PendingSessionMapper,
                                                            @AuthDatabase
                                                            private val realmConfiguration: RealmConfiguration
) : PendingSessionStore {

    override suspend fun savePendingSessionData(pendingSessionData: PendingSessionData) {
        awaitTransaction(realmConfiguration) { realm ->
            val entity = mapper.map(pendingSessionData)
            if (entity != null) {
                realm.where(PendingSessionEntity::class.java)
                        .findAll()
                        .deleteAllFromRealm()

                realm.insert(entity)
            }
        }
    }

    override fun getPendingSessionData(): PendingSessionData? {
        return Realm.getInstance(realmConfiguration).use { realm ->
            realm
                    .where(PendingSessionEntity::class.java)
                    .findAll()
                    .map { mapper.map(it) }
                    .firstOrNull()
        }
    }

    override suspend fun delete() {
        awaitTransaction(realmConfiguration) {
            it.where(PendingSessionEntity::class.java)
                    .findAll()
                    .deleteAllFromRealm()
        }
    }
}
