/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.util

import org.matrix.android.sdk.api.MatrixCallback
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend inline fun <T> awaitCallback(crossinline callback: (MatrixCallback<T>) -> Unit) = suspendCoroutine<T> { cont ->
    callback(object : MatrixCallback<T> {
        override fun onFailure(failure: Throwable) {
            cont.resumeWithException(failure)
        }

        override fun onSuccess(data: T) {
            cont.resume(data)
        }
    })
}
