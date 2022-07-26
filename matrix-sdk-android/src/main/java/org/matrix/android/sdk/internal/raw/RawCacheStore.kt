/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.raw

import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmSingleQuery
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.await
import org.matrix.android.sdk.internal.di.GlobalDatabase
import org.matrix.android.sdk.internal.raw.db.model.RawCacheEntity
import org.matrix.android.sdk.internal.util.time.Clock
import javax.inject.Inject

internal class RawCacheStore @Inject constructor(
        @GlobalDatabase private val realmInstance: RealmInstance,
        private val clock: Clock,
) {

    suspend fun getWithUrl(url: String): RawCacheEntity? {
        val realm = realmInstance.getRealm()
        return queryForUrl(realm, url).await()
    }

    suspend fun upsertWith(url: String, data: String) {
        realmInstance.write {
            val timestamp = clock.epochMillis()
            val rawCacheEntity = queryForUrl(this, url).find()
            if (rawCacheEntity != null) {
                rawCacheEntity.data = data
                rawCacheEntity.lastUpdatedTimestamp = timestamp
            } else {
                val newCacheEntity = RawCacheEntity()
                        .apply {
                            this.data = data
                            this.lastUpdatedTimestamp = timestamp
                        }
                copyToRealm(newCacheEntity)
            }
        }
    }

    private fun queryForUrl(realm: TypedRealm, url: String): RealmSingleQuery<RawCacheEntity> {
        return realm
                .query(RawCacheEntity::class, "url == $0", url)
                .first()
    }
}
