/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.cache

import io.realm.RealmConfiguration
import org.matrix.android.sdk.internal.database.awaitTransaction
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface ClearCacheTask : Task<Unit, Unit>

internal class RealmClearCacheTask @Inject constructor(private val realmConfiguration: RealmConfiguration) : ClearCacheTask {

    override suspend fun execute(params: Unit) {
        awaitTransaction(realmConfiguration) {
            it.deleteAll()
        }
    }
}
