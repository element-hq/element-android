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

package org.matrix.android.sdk.internal.session.identity.db

import io.realm.kotlin.UpdatePolicy
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.await
import org.matrix.android.sdk.internal.di.IdentityDatabase
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.identity.data.IdentityData
import org.matrix.android.sdk.internal.session.identity.data.IdentityPendingBinding
import org.matrix.android.sdk.internal.session.identity.data.IdentityStore
import org.matrix.android.sdk.internal.session.identity.db.IdentityPendingBindingEntity.Companion.toPrimaryKey
import org.matrix.android.sdk.internal.session.identity.model.IdentityHashDetailResponse
import javax.inject.Inject

@SessionScope
internal class RealmIdentityStore @Inject constructor(
        @IdentityDatabase
        private val realmInstance: RealmInstance,
) : IdentityStore {

    override suspend fun getIdentityData(): IdentityData? {
        return getIdentityDataEntity()
                ?.let {
                    IdentityMapper.map(it)
                }
    }

    override suspend fun setUrl(url: String?) {
        val identityData = getIdentityDataEntity() ?: return
        realmInstance.write {
            findLatest(identityData)?.identityServerUrl = url
        }
    }

    override suspend fun setToken(token: String?) {
        val identityData = getIdentityDataEntity() ?: return
        realmInstance.write {
            findLatest(identityData)?.token = token
        }
    }

    override suspend fun setUserConsent(consent: Boolean) {
        val identityData = getIdentityDataEntity() ?: return
        realmInstance.write {
            findLatest(identityData)?.userConsent = consent
        }
    }

    override suspend fun setHashDetails(hashDetailResponse: IdentityHashDetailResponse) {
        val identityData = getIdentityDataEntity() ?: return
        realmInstance.write {
            findLatest(identityData)?.apply {
                hashLookupAlgorithm.clear()
                hashLookupAlgorithm.addAll(hashDetailResponse.algorithms)
                hashLookupPepper = hashDetailResponse.pepper
            }
        }
    }

    override suspend fun storePendingBinding(threePid: ThreePid, data: IdentityPendingBinding) {
        realmInstance.write {
            val pendingBindingEntity = IdentityPendingBindingEntity().apply {
                this.threePid = threePid.toPrimaryKey()
                clientSecret = data.clientSecret
                sendAttempt = data.sendAttempt
                sid = data.sid
            }
            copyToRealm(pendingBindingEntity, updatePolicy = UpdatePolicy.ALL)
        }
    }

    override suspend fun getPendingBinding(threePid: ThreePid): IdentityPendingBinding? {
        return getPendingBindingEntity(threePid)?.let {
            IdentityMapper.map(it)
        }
    }

    override suspend fun deletePendingBinding(threePid: ThreePid) {
        val pendingBindingEntity = getPendingBindingEntity(threePid) ?: return
        realmInstance.write {
            findLatest(pendingBindingEntity)?.also {
                delete(it)
            }
        }
    }

    private suspend fun getPendingBindingEntity(threePid: ThreePid): IdentityPendingBindingEntity? {
        return realmInstance.getRealm()
                .query(IdentityPendingBindingEntity::class, "threePid == $0", threePid.toPrimaryKey())
                .first()
                .await()
    }

    private suspend fun getIdentityDataEntity(): IdentityDataEntity? {
        return realmInstance.getRealm()
                .query(IdentityDataEntity::class)
                .first()
                .await()
    }
}
