/*
 * Copyright (c) 2022 New Vector Ltd
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

package org.matrix.android.sdk.internal.auth

import io.realm.kotlin.UpdatePolicy
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.await
import org.matrix.android.sdk.internal.di.GlobalDatabase
import org.matrix.android.sdk.internal.raw.db.model.KnownServerUrlEntity
import javax.inject.Inject

internal class KnownServerUrlStore @Inject constructor(
        @GlobalDatabase private val realmInstance: RealmInstance,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
) {

    suspend fun getAll(): List<String> = withContext(coroutineDispatchers.io) {
        val realm = realmInstance.getRealm()
        realm.query(KnownServerUrlEntity::class)
                .await()
                .map { it.url }
    }

    suspend fun add(url: String) {
        realmInstance.write {
            val entity = KnownServerUrlEntity().apply {
                this.url = url
            }
            copyToRealm(entity, updatePolicy = UpdatePolicy.ALL)
        }
    }

    suspend fun deleteAll() {
        realmInstance.write {
            val entities = query(KnownServerUrlEntity::class).find()
            delete(entities)
        }
    }
}
