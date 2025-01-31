/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.extensions.foldToCallback
import org.matrix.android.sdk.internal.util.toCancelable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal fun <T> CoroutineScope.launchToCallback(
        context: CoroutineContext = EmptyCoroutineContext,
        callback: MatrixCallback<T>,
        block: suspend () -> T
): Cancelable = launch(context, CoroutineStart.DEFAULT) {
    val result = runCatching {
        block()
    }
    withContext(Dispatchers.Main) {
        result.foldToCallback(callback)
    }
}.toCancelable()
