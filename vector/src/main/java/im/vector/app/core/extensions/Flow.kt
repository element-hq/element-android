/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Returns a flow that invokes the given action after the first value of the upstream flow is emitted downstream.
 */
fun <T> Flow<T>.onFirst(action: (T) -> Unit): Flow<T> = flow {
    var emitted = false
    collect { value ->
        emit(value) // always emit value

        if (!emitted) {
            action(value) // execute the action after the first emission
            emitted = true
        }
    }
}
