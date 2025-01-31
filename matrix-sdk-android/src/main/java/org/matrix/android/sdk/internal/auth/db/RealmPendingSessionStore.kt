/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.db

import io.realm.Realm
import io.realm.RealmConfiguration
import org.matrix.android.sdk.internal.auth.PendingSessionStore
import org.matrix.android.sdk.internal.database.awaitTransaction
import org.matrix.android.sdk.internal.di.AuthDatabase
import javax.inject.Inject

internal class RealmPendingSessionStore @Inject constructor(
        private val mapper: PendingSessionMapper,
        @AuthDatabase
        private val realmConfiguration: RealmConfiguration
) : PendingSessionStore {

    override suspend fun savePendingSessionData(pendingSessionData: PendingSessionData) {
        awaitTransaction(realmConfiguration) { realm ->
            val entity = mapper.map(pendingSessionData)
            if (entity != null) {
                realm.where(PendingSessionEntity::class.java)
                        .findAll()
                        .deleteAllFromRealm()

                realm.insert(entity)
            }
        }
    }

    override fun getPendingSessionData(): PendingSessionData? {
        return Realm.getInstance(realmConfiguration).use { realm ->
            realm
                    .where(PendingSessionEntity::class.java)
                    .findAll()
                    .map { mapper.map(it) }
                    .firstOrNull()
        }
    }

    override suspend fun delete() {
        awaitTransaction(realmConfiguration) {
            it.where(PendingSessionEntity::class.java)
                    .findAll()
                    .deleteAllFromRealm()
        }
    }
}
