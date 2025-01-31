/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import androidx.test.platform.app.InstrumentationRegistry
import im.vector.app.features.room.VectorRoomDisplayNameFallbackProvider
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixConfiguration

fun getMatrixInstance(): Matrix {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val configuration = MatrixConfiguration(
            roomDisplayNameFallbackProvider = VectorRoomDisplayNameFallbackProvider(context)
    )
    return Matrix(context, configuration)
}
