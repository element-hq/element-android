/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import androidx.test.platform.app.InstrumentationRegistry
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.SyncConfig

fun getMatrixInstance(): Matrix {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val configuration = MatrixConfiguration(
            roomDisplayNameFallbackProvider = TestRoomDisplayNameFallbackProvider(),
            syncConfig = SyncConfig(longPollTimeout = 5_000L),
    )
    return Matrix(context, configuration)
}
