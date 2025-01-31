/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk

import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.asCoroutineDispatcher
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import java.util.concurrent.Executors

internal val testCoroutineDispatchers = MatrixCoroutineDispatchers(
        Main, Main, Main, Main,
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
)
