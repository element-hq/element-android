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

import io.realm.kotlin.UpdatePolicy
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.auth.data.sessionId
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.di.AuthDatabase
import timber.log.Timber
import javax.inject.Inject

internal class RealmSessionParamsStore @Inject constructor(
        private val mapper: SessionParamsMapper,
        @AuthDatabase
        private val realmInstance: RealmInstance
) : SessionParamsStore {

    override fun getLast(): SessionParams? {
        return realmInstance.blockingRealm()
                .query(SessionParamsEntity::class)
                .find()
                .map { mapper.map(it) }
                .lastOrNull()
    }

    override fun get(sessionId: String): SessionParams? {
        return realmInstance.blockingRealm()
                .query(SessionParamsEntity::class)
                .query("sessionId == $0", sessionId)
                .first()
                .find()
                ?.let { mapper.map(it) }
    }

    override fun getAll(): List<SessionParams> {
        return realmInstance.blockingRealm()
                .query(SessionParamsEntity::class)
                .find()
                .mapNotNull { mapper.map(it) }
    }

    override suspend fun save(sessionParams: SessionParams) {
        realmInstance.write {
            val entity = mapper.map(sessionParams)
            if (entity != null) {
                copyToRealm(entity, updatePolicy = UpdatePolicy.ALL)
            }
        }
    }

    override suspend fun setTokenInvalid(sessionId: String) {
        realmInstance.write {
            val currentSessionParams =
                    query(SessionParamsEntity::class)
                            .query("sessionId == $0", sessionId)
                            .first()
                            .find()

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
        realmInstance.write {
            val currentSessionParams = query(SessionParamsEntity::class)
                    .query("sessionId == $0", newCredentials.sessionId())
                    .first()
                    .find()
                    ?.let { mapper.map(it) }

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
                    copyToRealm(entity, updatePolicy = UpdatePolicy.ALL)
                }
            }
        }
    }

    override suspend fun delete(sessionId: String) {
        realmInstance.write {
            val sessionParam = query(SessionParamsEntity::class)
                    .query("sessionId == $0", sessionId)
                    .find()
            delete(sessionParam)
        }
    }

    override suspend fun deleteAll() {
        realmInstance.write {
            val sessionParam = query(SessionParamsEntity::class).find()
            delete(sessionParam)
        }
    }
}
