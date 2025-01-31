/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.util

import com.squareup.moshi.Moshi
import org.matrix.android.sdk.internal.di.MoshiProvider

/**
 * Entry point to get a Json parser.
 */
object MatrixJsonParser {
    /**
     * @return a Moshi Json parser instance, configured to handle some Matrix Event contents
     */
    fun getMoshi(): Moshi {
        return MoshiProvider.providesMoshi()
    }
}
