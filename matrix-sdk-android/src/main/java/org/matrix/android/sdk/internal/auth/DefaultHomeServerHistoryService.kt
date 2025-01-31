/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth

import com.zhuinden.monarchy.Monarchy
import io.realm.kotlin.where
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.internal.database.model.KnownServerUrlEntity
import org.matrix.android.sdk.internal.di.GlobalDatabase
import javax.inject.Inject

internal class DefaultHomeServerHistoryService @Inject constructor(
        @GlobalDatabase private val monarchy: Monarchy
) : HomeServerHistoryService {

    override fun getKnownServersUrls(): List<String> {
        return monarchy.fetchAllMappedSync(
                { realm ->
                    realm.where<KnownServerUrlEntity>()
                },
                { it.url }
        )
    }

    override fun addHomeServerToHistory(url: String) {
        monarchy.writeAsync { realm ->
            KnownServerUrlEntity(url).let {
                realm.insertOrUpdate(it)
            }
        }
    }

    override fun clearHistory() {
        monarchy.runTransactionSync { it.where<KnownServerUrlEntity>().findAll().deleteAllFromRealm() }
    }
}
