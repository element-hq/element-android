/*
  * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.internal.database

import io.realm.Realm
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


suspend fun Realm.awaitTransaction(transaction: (realm: Realm) -> Unit) {
    return suspendCancellableCoroutine { continuation ->
        beginTransaction()
        try {
            transaction(this)
            commitTransaction()
            continuation.resume(Unit)
        } catch (e: Throwable) {
            if (isInTransaction) {
                cancelTransaction()
            } else {
                Timber.w("Could not cancel transaction, not currently in a transaction.")
            }
            continuation.resumeWithException(e)
        }
        continuation.invokeOnCancellation {
            if (isInTransaction) {
                cancelTransaction()
            }
        }
    }
}
