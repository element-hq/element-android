/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.util

/**
 * An interface defining a unique cancel method.
 * It should be used with methods you want to be able to cancel, such as ones interacting with Web Services.
 */
interface Cancelable {

    /**
     * The cancel method, it does nothing by default.
     */
    fun cancel() {
        // no-op
    }
}

object NoOpCancellable : Cancelable
