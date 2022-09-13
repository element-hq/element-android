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

package org.matrix.android.sdk.internal.auth

import com.zhuinden.monarchy.Monarchy
import io.realm.kotlin.where
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.internal.database.model.KnownServerUrlEntity
import org.matrix.android.sdk.internal.di.GlobalDatabase
import javax.inject.Inject

internal class DefaultHomeServerHistoryService @Inject constructor(
        @GlobalDatabase private val monarchy: Monarchy
) : HomeServerHistoryService {

    override fun getKnownServersUrls(): List<String> {
        return monarchy.fetchAllMappedSync(
                { realm ->
                    realm.where<KnownServerUrlEntity>()
                },
                { it.url }
        )
    }

    override fun addHomeServerToHistory(url: String) {
        monarchy.writeAsync { realm ->
            KnownServerUrlEntity(url).let {
                realm.insertOrUpdate(it)
            }
        }
    }

    override fun clearHistory() {
        monarchy.runTransactionSync { it.where<KnownServerUrlEntity>().findAll().deleteAllFromRealm() }
    }
}
