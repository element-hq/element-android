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

package im.vector.matrix.android.internal.util

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.awaitTransaction
import io.realm.Realm
import io.realm.RealmModel
import java.util.concurrent.atomic.AtomicReference

internal suspend fun Monarchy.awaitTransaction(transaction: suspend (realm: Realm) -> Unit) {
    awaitTransaction(realmConfiguration, transaction)
}

fun <T : RealmModel> Monarchy.fetchCopied(query: (Realm) -> T?): T? {
    val ref = AtomicReference<T>()
    doWithRealm { realm ->
        val result = query.invoke(realm)?.let {
            realm.copyFromRealm(it)
        }
        ref.set(result)
    }
    return ref.get()
}

fun <U, T : RealmModel> Monarchy.fetchCopyMap(query: (Realm) -> T?, map: (T, realm: Realm) -> U): U? {
    val ref = AtomicReference<U?>()
    doWithRealm { realm ->
        val result = query.invoke(realm)?.let {
            map(realm.copyFromRealm(it), realm)
        }
        ref.set(result)
    }
    return ref.get()
}
