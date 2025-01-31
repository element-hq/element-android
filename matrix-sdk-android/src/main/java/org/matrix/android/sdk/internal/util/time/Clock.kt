/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util.time

import javax.inject.Inject

internal interface Clock {
    fun epochMillis(): Long
}

internal class DefaultClock @Inject constructor() : Clock {

    /**
     * Provides a UTC epoch in milliseconds
     *
     * This value is not guaranteed to be correct with reality
     * as a User can override the system time and date to any values.
     */
    override fun epochMillis(): Long {
        return System.currentTimeMillis()
    }
}
