/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.error

import timber.log.Timber

/**
 * throw in debug, only log in production. As this method does not always throw, next statement should be a return
 */
fun fatalError(message: String, failFast: Boolean) {
    if (failFast) {
        error(message)
    } else {
        Timber.e(message)
    }
}
