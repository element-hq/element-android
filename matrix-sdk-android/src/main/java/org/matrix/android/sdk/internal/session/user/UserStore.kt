/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.user

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.internal.database.model.UserEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.util.awaitTransaction
import javax.inject.Inject

internal interface UserStore {
    suspend fun createOrUpdate(userId: String, displayName: String? = null, avatarUrl: String? = null)
    suspend fun updateAvatar(userId: String, avatarUrl: String? = null)
    suspend fun updateDisplayName(userId: String, displayName: String? = null)
}

internal class RealmUserStore @Inject constructor(@SessionDatabase private val monarchy: Monarchy) : UserStore {

    override suspend fun createOrUpdate(userId: String, displayName: String?, avatarUrl: String?) {
        monarchy.awaitTransaction {
            val userEntity = UserEntity(userId, displayName ?: "", avatarUrl ?: "")
            it.insertOrUpdate(userEntity)
        }
    }

    override suspend fun updateAvatar(userId: String, avatarUrl: String?) {
        monarchy.awaitTransaction { realm ->
            UserEntity.where(realm, userId).findFirst()?.let {
                it.avatarUrl = avatarUrl ?: ""
            }
        }
    }

    override suspend fun updateDisplayName(userId: String, displayName: String?) {
        monarchy.awaitTransaction { realm ->
            UserEntity.where(realm, userId).findFirst()?.let {
                it.displayName = displayName ?: ""
            }
        }
    }
}
