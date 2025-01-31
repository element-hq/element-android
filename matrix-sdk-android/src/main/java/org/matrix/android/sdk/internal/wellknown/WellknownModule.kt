/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.wellknown

import dagger.Binds
import dagger.Module

@Module
internal abstract class WellknownModule {

    @Binds
    abstract fun bindGetWellknownTask(task: DefaultGetWellknownTask): GetWellknownTask
}
