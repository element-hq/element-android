/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.internal.database.model.SyncEntity
import org.matrix.android.sdk.internal.di.SessionDatabase
import javax.inject.Inject

internal class SyncTokenStore @Inject constructor(@SessionDatabase private val monarchy: Monarchy) {

    fun getLastToken(): String? {
        val token = Realm.getInstance(monarchy.realmConfiguration).use {
            // Makes sure realm is up-to-date as it's used for querying internally on non looper thread.
            it.refresh()
            it.where(SyncEntity::class.java).findFirst()?.nextBatch
        }
        return token
    }

    fun saveToken(realm: Realm, token: String?) {
        val sync = SyncEntity(token)
        realm.insertOrUpdate(sync)
    }
}
