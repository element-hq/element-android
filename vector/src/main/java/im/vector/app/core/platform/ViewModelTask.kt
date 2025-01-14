/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

interface ViewModelTask<Params, Result> {
    operator fun invoke(
            scope: CoroutineScope,
            params: Params,
            onResult: (Result) -> Unit = {}
    ) {
        val backgroundJob = scope.async { execute(params) }
        scope.launch { onResult(backgroundJob.await()) }
    }

    suspend fun execute(params: Params): Result
}
