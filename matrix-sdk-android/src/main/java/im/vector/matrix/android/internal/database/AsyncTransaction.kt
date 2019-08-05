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
import io.realm.RealmConfiguration
import io.realm.internal.OsSharedRealm
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private object AsyncTransactionThreadHolder {

    val EXECUTOR: ExecutorService by lazy {
        Executors.newSingleThreadExecutor()
    }

}

private class AsyncTransactionRunnable(private val continuation: CancellableContinuation<Unit>,
                                       private val realmConfiguration: RealmConfiguration,
                                       private val transaction: (realm: Realm) -> Unit) : Runnable {

    override fun run() {
        if (Thread.currentThread().isInterrupted) {
            return
        }
        var versionID: OsSharedRealm.VersionID? = null
        var exception: Throwable? = null

        val bgRealm = Realm.getInstance(realmConfiguration)
        bgRealm.beginTransaction()
        try {
            transaction(bgRealm)
            if (Thread.currentThread().isInterrupted) {
                return
            }
            bgRealm.commitTransaction()
            versionID = bgRealm.sharedRealm.versionID
        } catch (e: Throwable) {
            exception = e
        } finally {
            try {
                if (bgRealm.isInTransaction) {
                    bgRealm.cancelTransaction()
                }
            } finally {
                bgRealm.close()
            }
        }
        val backgroundException = exception
        val backgroundVersionID = versionID
        when {
            backgroundVersionID != null -> continuation.resume(Unit)
            backgroundException != null -> continuation.resumeWithException(backgroundException)
        }
    }

}

suspend fun awaitTransaction(realmConfiguration: RealmConfiguration, transaction: (realm: Realm) -> Unit) {
    return suspendCancellableCoroutine { continuation ->
        var futureTask: Future<*>? = null
        continuation.invokeOnCancellation {
            Timber.v("Cancel database transaction")
            futureTask?.cancel(true)
        }
        val runnable = AsyncTransactionRunnable(continuation, realmConfiguration, transaction)
        futureTask = AsyncTransactionThreadHolder.EXECUTOR.submit(runnable)
    }

}