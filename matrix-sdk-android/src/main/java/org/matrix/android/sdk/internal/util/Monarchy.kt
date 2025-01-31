/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
