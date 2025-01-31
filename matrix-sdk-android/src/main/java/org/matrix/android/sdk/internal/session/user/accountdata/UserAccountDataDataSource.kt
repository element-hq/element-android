/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.user.accountdata

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.RealmQuery
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataEvent
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.database.RealmSessionProvider
import org.matrix.android.sdk.internal.database.mapper.AccountDataMapper
import org.matrix.android.sdk.internal.database.model.UserAccountDataEntity
import org.matrix.android.sdk.internal.database.model.UserAccountDataEntityFields
import org.matrix.android.sdk.internal.di.SessionDatabase
import javax.inject.Inject

internal class UserAccountDataDataSource @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        private val realmSessionProvider: RealmSessionProvider,
        private val accountDataMapper: AccountDataMapper
) {

    fun getAccountDataEvent(type: String): UserAccountDataEvent? {
        return getAccountDataEvents(setOf(type)).firstOrNull()
    }

    fun getLiveAccountDataEvent(type: String): LiveData<Optional<UserAccountDataEvent>> {
        return Transformations.map(getLiveAccountDataEvents(setOf(type))) {
            it.firstOrNull().toOptional()
        }
    }

    fun getAccountDataEvents(types: Set<String>): List<UserAccountDataEvent> {
        return realmSessionProvider.withRealm {
            accountDataEventsQuery(it, types).findAll().map(accountDataMapper::map)
        }
    }

    fun getLiveAccountDataEvents(types: Set<String>): LiveData<List<UserAccountDataEvent>> {
        return monarchy.findAllMappedWithChanges(
                { accountDataEventsQuery(it, types) },
                accountDataMapper::map
        )
    }

    private fun accountDataEventsQuery(realm: Realm, types: Set<String>): RealmQuery<UserAccountDataEntity> {
        val query = realm.where(UserAccountDataEntity::class.java)
        if (types.isNotEmpty()) {
            query.`in`(UserAccountDataEntityFields.TYPE, types.toTypedArray())
        }
        return query
    }
}
