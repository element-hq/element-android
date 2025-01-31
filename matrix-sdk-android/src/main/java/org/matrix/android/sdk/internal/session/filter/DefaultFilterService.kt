/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.filter

import org.matrix.android.sdk.api.session.sync.FilterService
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import javax.inject.Inject

internal class DefaultFilterService @Inject constructor(
        private val saveFilterTask: SaveFilterTask,
        private val taskExecutor: TaskExecutor
) : FilterService {

    // TODO Pass a list of support events instead
    override fun setFilter(filterPreset: FilterService.FilterPreset) {
        saveFilterTask
                .configureWith(SaveFilterTask.Params(filterPreset))
                .executeBy(taskExecutor)
    }
}
