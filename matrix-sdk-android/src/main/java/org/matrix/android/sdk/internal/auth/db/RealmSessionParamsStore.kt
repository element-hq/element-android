/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.auth.db

import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.exceptions.RealmPrimaryKeyConstraintException
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.auth.data.sessionId
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.database.awaitTransaction
import org.matrix.android.sdk.internal.di.AuthDatabase
import timber.log.Timber
import javax.inject.Inject

internal class RealmSessionParamsStore @Inject constructor(private val mapper: SessionParamsMapper,
                                                           @AuthDatabase
                                                           private val realmConfiguration: RealmConfiguration
) : SessionParamsStore {

    override fun getLast(): SessionParams? {
        return Realm.getInstance(realmConfiguration).use { realm ->
            realm
                    .where(SessionParamsEntity::class.java)
                    .findAll()
                    .map { mapper.map(it) }
                    .lastOrNull()
        }
    }

    override fun get(sessionId: String): SessionParams? {
        return Realm.getInstance(realmConfiguration).use { realm ->
            realm
                    .where(SessionParamsEntity::class.java)
                    .equalTo(SessionParamsEntityFields.SESSION_ID, sessionId)
                    .findAll()
                    .map { mapper.map(it) }
                    .firstOrNull()
        }
    }

    override fun getAll(): List<SessionParams> {
        return Realm.getInstance(realmConfiguration).use { realm ->
            realm
                    .where(SessionParamsEntity::class.java)
                    .findAll()
                    .mapNotNull { mapper.map(it) }
        }
    }

    override suspend fun save(sessionParams: SessionParams) {
        awaitTransaction(realmConfiguration) {
            val entity = mapper.map(sessionParams)
            if (entity != null) {
                try {
                    it.insert(entity)
                } catch (e: RealmPrimaryKeyConstraintException) {
                    Timber.e(e, "Something wrong happened during previous session creation. Override with new credentials")
                    it.insertOrUpdate(entity)
                }
            }
        }
    }

    override suspend fun setTokenInvalid(sessionId: String) {
        awaitTransaction(realmConfiguration) { realm ->
            val currentSessionParams = realm
                    .where(SessionParamsEntity::class.java)
                    .equalTo(SessionParamsEntityFields.SESSION_ID, sessionId)
                    .findAll()
                    .firstOrNull()

            if (currentSessionParams == null) {
                // Should not happen
                "Session param not found for id $sessionId"
                        .let { Timber.w(it) }
                        .also { error(it) }
            } else {
                currentSessionParams.isTokenValid = false
            }
        }
    }

    override suspend fun updateCredentials(newCredentials: Credentials) {
        awaitTransaction(realmConfiguration) { realm ->
            val currentSessionParams = realm
                    .where(SessionParamsEntity::class.java)
                    .equalTo(SessionParamsEntityFields.SESSION_ID, newCredentials.sessionId())
                    .findAll()
                    .map { mapper.map(it) }
                    .firstOrNull()

            if (currentSessionParams == null) {
                // Should not happen
                "Session param not found for id ${newCredentials.sessionId()}"
                        .let { Timber.w(it) }
                        .also { error(it) }
            } else {
                val newSessionParams = currentSessionParams.copy(
                        credentials = newCredentials,
                        isTokenValid = true
                )

                val entity = mapper.map(newSessionParams)
                if (entity != null) {
                    realm.insertOrUpdate(entity)
                }
            }
        }
    }

    override suspend fun delete(sessionId: String) {
        awaitTransaction(realmConfiguration) {
            it.where(SessionParamsEntity::class.java)
                    .equalTo(SessionParamsEntityFields.SESSION_ID, sessionId)
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
