/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.debug

import okhttp3.Interceptor
import org.matrix.android.sdk.api.Matrix

interface FlipperProxy {
    fun init(matrix: Matrix)
    fun networkInterceptor(): Interceptor?
}
