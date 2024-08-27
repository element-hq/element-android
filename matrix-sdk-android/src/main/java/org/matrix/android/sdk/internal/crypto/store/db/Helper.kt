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

package org.matrix.android.sdk.internal.crypto.store.db

import io.realm.Realm
import io.realm.RealmConfiguration
import timber.log.Timber
import kotlin.system.measureTimeMillis

/**
 * Get realm, invoke the action, close realm, and return the result of the action.
 */
internal fun <T> doWithRealm(realmConfiguration: RealmConfiguration, action: (Realm) -> T): T {
    return Realm.getInstance(realmConfiguration).use { realm ->
        action.invoke(realm)
    }
}

/**
 * Get realm instance, invoke the action in a transaction and close realm.
 */
internal fun doRealmTransaction(tag: String, realmConfiguration: RealmConfiguration, action: (Realm) -> Unit) {
    measureTimeMillis {
        Realm.getInstance(realmConfiguration).use { realm ->
            realm.executeTransaction { action.invoke(it) }
        }
    }.also { Timber.w("doRealmTransaction for $tag took $it millis") }
}

internal fun doRealmTransactionAsync(realmConfiguration: RealmConfiguration, action: (Realm) -> Unit) {
    Realm.getInstance(realmConfiguration).use { realm ->
        realm.executeTransactionAsync { action.invoke(it) }
    }
}
