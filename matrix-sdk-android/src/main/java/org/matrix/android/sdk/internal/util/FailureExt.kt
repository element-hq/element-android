/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.internal.di.MoshiProvider

/**
 * Try to extract and serialize a MatrixError, or default to localizedMessage.
 */
internal fun Throwable.toMatrixErrorStr(): String {
    return (this as? Failure.ServerError)
            ?.let {
                // Serialize the MatrixError in this case
                val adapter = MoshiProvider.providesMoshi().adapter(MatrixError::class.java)
                tryOrNull { adapter.toJson(error) }
            }
            ?: localizedMessage
            ?: "error"
}
