/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.listeners

/**
 * Interface to send a progress info.
 */
interface ProgressListener {
    /**
     * Will be invoked on the background thread, not in UI thread.
     * @param progress from 0 to total by contract
     * @param total
     */
    fun onProgress(progress: Int, total: Int)
}
