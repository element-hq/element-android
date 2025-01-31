/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.extensions

import org.matrix.android.sdk.api.MatrixCallback

internal fun <A> Result<A>.foldToCallback(callback: MatrixCallback<A>): Unit = fold(
        { callback.onSuccess(it) },
        { callback.onFailure(it) }
)
