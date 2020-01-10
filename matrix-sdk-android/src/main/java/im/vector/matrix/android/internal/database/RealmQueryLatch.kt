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

package im.vector.matrix.android.internal.database

import io.realm.*
import kotlinx.coroutines.*

internal suspend fun <T> awaitNotEmptyResult(realmConfiguration: RealmConfiguration,
                                    timeoutMillis: Long,
                                    builder: (Realm) -> RealmQuery<T>) {
    withTimeout(timeoutMillis) {
        // Confine Realm interaction to a single thread with Looper.
        withContext(Dispatchers.Main) {
            val latch = CompletableDeferred<Unit>()

            Realm.getInstance(realmConfiguration).use { realm ->
                val result = builder(realm).findAllAsync()

                val listener = object : RealmChangeListener<RealmResults<T>> {
                    override fun onChange(it: RealmResults<T>) {
                        if (it.isNotEmpty()) {
                            result.removeChangeListener(this)
                            latch.complete(Unit)
                        }
                    }
                }

                result.addChangeListener(listener)
                try {
                    latch.await()
                } catch (e: CancellationException) {
                    result.removeChangeListener(listener)
                    throw e
                }
            }
        }
    }
}
