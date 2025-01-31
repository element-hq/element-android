/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.media

import com.zhuinden.monarchy.Monarchy
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.PreviewUrlCacheEntity
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import javax.inject.Inject

internal interface ClearPreviewUrlCacheTask : Task<Unit, Unit>

internal class DefaultClearPreviewUrlCacheTask @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy
) : ClearPreviewUrlCacheTask {

    override suspend fun execute(params: Unit) {
        monarchy.awaitTransaction { realm ->
            realm.where<PreviewUrlCacheEntity>()
                    .findAll()
                    .deleteAllFromRealm()
        }
    }
}
