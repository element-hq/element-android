/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.widgets.token

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.internal.database.model.ScalarTokenEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.util.awaitTransaction
import org.matrix.android.sdk.internal.util.fetchCopyMap
import javax.inject.Inject

internal class ScalarTokenStore @Inject constructor(@SessionDatabase private val monarchy: Monarchy) {

    fun getToken(apiUrl: String): String? {
        return monarchy.fetchCopyMap({ realm ->
            ScalarTokenEntity.where(realm, apiUrl).findFirst()
        }, { scalarToken, _ ->
            scalarToken.token
        })
    }

    suspend fun setToken(apiUrl: String, token: String) {
        monarchy.awaitTransaction { realm ->
            val scalarTokenEntity = ScalarTokenEntity(apiUrl, token)
            realm.insertOrUpdate(scalarTokenEntity)
        }
    }

    suspend fun clearToken(apiUrl: String) {
        monarchy.awaitTransaction { realm ->
            ScalarTokenEntity.where(realm, apiUrl).findFirst()?.deleteFromRealm()
        }
    }
}
