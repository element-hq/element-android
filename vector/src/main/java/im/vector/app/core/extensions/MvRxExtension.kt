/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success

/**
 * It maybe already exist somewhere but I cannot find it.
 */
suspend fun <T> tryAsync(block: suspend () -> T): Async<T> {
    return try {
        Success(block.invoke())
    } catch (failure: Throwable) {
        Fail(failure)
    }
}
