/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.mvrx

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success

/**
 * Note: this will be removed when upgrading to mvrx2.
 */
suspend fun <A> runCatchingToAsync(block: suspend () -> A): Async<A> {
    return runCatching {
        block.invoke()
    }.fold(
            { Success(it) },
            { Fail(it) }
    )
}
