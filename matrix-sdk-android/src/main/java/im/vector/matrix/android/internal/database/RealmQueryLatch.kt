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

import im.vector.matrix.android.internal.util.createBackgroundHandler
import io.realm.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class RealmQueryLatch<E : RealmObject>(private val realmConfiguration: RealmConfiguration,
                                       private val realmQueryBuilder: (Realm) -> RealmQuery<E>) {

    private companion object {
        val QUERY_LATCH_HANDLER = createBackgroundHandler("REALM_QUERY_LATCH")
    }

    @Throws(InterruptedException::class)
    fun await(timeout: Long, timeUnit: TimeUnit) {
        val realmRef = AtomicReference<Realm>()
        val latch = CountDownLatch(1)
        QUERY_LATCH_HANDLER.post {
            val realm = Realm.getInstance(realmConfiguration)
            realmRef.set(realm)
            val result = realmQueryBuilder(realm).findAllAsync()
            result.addChangeListener(object : RealmChangeListener<RealmResults<E>> {
                override fun onChange(t: RealmResults<E>) {
                    if (t.isNotEmpty()) {
                        result.removeChangeListener(this)
                        latch.countDown()
                    }
                }
            })
        }
        try {
            latch.await(timeout, timeUnit)
        } catch (exception: InterruptedException) {
            throw exception
        } finally {
            QUERY_LATCH_HANDLER.post {
                realmRef.getAndSet(null).close()
            }
        }
    }
}
