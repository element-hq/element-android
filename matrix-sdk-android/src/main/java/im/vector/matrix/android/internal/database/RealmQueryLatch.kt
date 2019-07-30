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

import android.os.Handler
import android.os.HandlerThread
import io.realm.*
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val THREAD_NAME = "REALM_QUERY_LATCH"

class RealmQueryLatch<E : RealmObject>(private val realmConfiguration: RealmConfiguration,
                                       private val realmQueryBuilder: (Realm) -> RealmQuery<E>) {

    @Throws(InterruptedException::class)
    fun await(timeout: Long = Long.MAX_VALUE, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
        val latch = CountDownLatch(1)
        val handlerThread = HandlerThread(THREAD_NAME + hashCode())
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        val runnable = Runnable {
            val realm = Realm.getInstance(realmConfiguration)
            val result = realmQueryBuilder(realm).findAllAsync()

            result.addChangeListener(object : RealmChangeListener<RealmResults<E>> {
                override fun onChange(t: RealmResults<E>) {
                    if (t.isNotEmpty()) {
                        result.removeChangeListener(this)
                        realm.close()
                        latch.countDown()
                    }
                }
            })
        }
        handler.post(runnable)
        try {
            latch.await(timeout, timeUnit)
        } catch (exception: InterruptedException) {
            throw exception
        } finally {
            handlerThread.quit()
        }
    }


}