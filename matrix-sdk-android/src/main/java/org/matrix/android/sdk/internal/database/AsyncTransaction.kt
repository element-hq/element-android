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
package org.matrix.android.sdk.internal.database

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

internal fun <T> CoroutineScope.asyncTransaction(monarchy: Monarchy, transaction: suspend (realm: Realm) -> T) {
    asyncTransaction(monarchy.realmConfiguration, transaction)
}

internal fun <T> CoroutineScope.asyncTransaction(realmConfiguration: RealmConfiguration, transaction: suspend (realm: Realm) -> T) {
    launch {
        awaitTransaction(realmConfiguration, transaction)
    }
}

internal suspend fun <T> awaitTransaction(config: RealmConfiguration, transaction: suspend (realm: Realm) -> T): T {
    return withContext(Realm.WRITE_EXECUTOR.asCoroutineDispatcher()) {
        Realm.getInstance(config).use { bgRealm ->
            bgRealm.beginTransaction()
            val result: T
            try {
                val start = System.currentTimeMillis()
                result = transaction(bgRealm)
                if (isActive) {
                    bgRealm.commitTransaction()
                    val end = System.currentTimeMillis()
                    val time = end - start
                    Timber.v("Execute transaction in $time millis")
                }
            } finally {
                if (bgRealm.isInTransaction) {
                    bgRealm.cancelTransaction()
                }
            }
            result
        }
    }
}
