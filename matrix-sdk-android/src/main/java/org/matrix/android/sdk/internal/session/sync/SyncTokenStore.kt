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

package org.matrix.android.sdk.internal.session.sync

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.internal.database.model.SyncEntity
import org.matrix.android.sdk.internal.di.SessionDatabase
import javax.inject.Inject

internal class SyncTokenStore @Inject constructor(@SessionDatabase private val monarchy: Monarchy) {

    fun getLastToken(): String? {
        val token = Realm.getInstance(monarchy.realmConfiguration).use {
            // Makes sure realm is up-to-date as it's used for querying internally on non looper thread.
            it.refresh()
            it.where(SyncEntity::class.java).findFirst()?.nextBatch
        }
        return token
    }

    fun saveToken(realm: Realm, token: String?) {
        val sync = SyncEntity(token)
        realm.insertOrUpdate(sync)
    }
}
