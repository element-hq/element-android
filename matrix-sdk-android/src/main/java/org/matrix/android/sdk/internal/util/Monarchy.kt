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

package org.matrix.android.sdk.internal.util

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.RealmModel
import org.matrix.android.sdk.internal.database.awaitTransaction
import java.util.concurrent.atomic.AtomicReference

internal suspend fun <T> Monarchy.awaitTransaction(transaction: suspend (realm: Realm) -> T): T {
    return awaitTransaction(realmConfiguration, transaction)
}

internal fun <T : RealmModel> Monarchy.fetchCopied(query: (Realm) -> T?): T? {
    val ref = AtomicReference<T>()
    doWithRealm { realm ->
        val result = query.invoke(realm)?.let {
            realm.copyFromRealm(it)
        }
        ref.set(result)
    }
    return ref.get()
}

internal fun <U, T : RealmModel> Monarchy.fetchCopyMap(query: (Realm) -> T?, map: (T, realm: Realm) -> U): U? {
    val ref = AtomicReference<U?>()
    doWithRealm { realm ->
        val result = query.invoke(realm)?.let {
            map(realm.copyFromRealm(it), realm)
        }
        ref.set(result)
    }
    return ref.get()
}
