/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.flow

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

internal fun <T> Flow<T>.startWith(dispatcher: CoroutineDispatcher, supplier: suspend () -> T): Flow<T> {
    return onStart {
        val value = withContext(dispatcher) {
            supplier()
        }
        emit(value)
    }
}
