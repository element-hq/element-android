/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util

import org.matrix.android.sdk.BuildConfig
import timber.log.Timber

/**
 * Throws in debug, only log in production.
 * As this method does not always throw, next statement should be a return.
 */
internal fun fatalError(message: String) {
    if (BuildConfig.DEBUG) {
        error(message)
    } else {
        Timber.e(message)
    }
}
