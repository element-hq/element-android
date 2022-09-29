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

package org.matrix.android.sdk.internal.session.widgets.token

import io.realm.kotlin.UpdatePolicy
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.model.ScalarTokenEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import javax.inject.Inject

internal class ScalarTokenStore @Inject constructor(@SessionDatabase private val realmInstance: RealmInstance) {

    fun getToken(apiUrl: String): String? {
        val realm = realmInstance.getBlockingRealm()
        return ScalarTokenEntity.where(realm, apiUrl)
                .first()
                .find()
                ?.token
    }

    suspend fun setToken(apiUrl: String, token: String) {
        realmInstance.write {
            val scalarTokenEntity = ScalarTokenEntity().apply {
                this.serverUrl = apiUrl
                this.token = token
            }
            copyToRealm(scalarTokenEntity, updatePolicy = UpdatePolicy.ALL)
        }
    }

    suspend fun clearToken(apiUrl: String) {
        realmInstance.write {
            ScalarTokenEntity.where(this, apiUrl).first().find()?.also {
                delete(it)
            }
        }
    }
}
