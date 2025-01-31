/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.util

interface BuildVersionSdkIntProvider {
    /**
     * Return the current version of the Android SDK.
     */
    fun get(): Int

    /**
     * Checks the if the current OS version is equal or greater than [version].
     * @return A `non-null` result if true, `null` otherwise.
     */
    fun <T> whenAtLeast(version: Int, result: () -> T): T? {
        return if (get() >= version) {
            result()
        } else null
    }
}
