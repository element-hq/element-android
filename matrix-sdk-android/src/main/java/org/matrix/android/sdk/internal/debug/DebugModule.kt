/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.debug

import dagger.Binds
import dagger.Module
import org.matrix.android.sdk.api.debug.DebugService

@Module
internal abstract class DebugModule {

    @Binds
    abstract fun bindDebugService(service: DefaultDebugService): DebugService
}
