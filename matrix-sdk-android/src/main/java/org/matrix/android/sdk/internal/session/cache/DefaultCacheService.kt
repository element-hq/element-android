/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.cache

import org.matrix.android.sdk.api.session.cache.CacheService
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.task.TaskExecutor
import javax.inject.Inject

internal class DefaultCacheService @Inject constructor(
        @SessionDatabase private val clearCacheTask: ClearCacheTask,
        private val taskExecutor: TaskExecutor
) : CacheService {

    override suspend fun clearCache() {
        taskExecutor.cancelAll()
        clearCacheTask.execute(Unit)
    }
}
