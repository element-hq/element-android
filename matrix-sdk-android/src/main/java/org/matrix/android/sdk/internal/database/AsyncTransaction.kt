/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import kotlin.system.measureTimeMillis

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
                measureTimeMillis {
                    result = transaction(bgRealm)
                    if (isActive) {
                        bgRealm.commitTransaction()
                    }
                }.also {
                    Timber.v("Execute transaction in $it millis")
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
