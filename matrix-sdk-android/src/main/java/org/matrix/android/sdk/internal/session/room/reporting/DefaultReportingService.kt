/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.reporting

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.matrix.android.sdk.api.session.room.reporting.ReportingService

internal class DefaultReportingService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val reportContentTask: ReportContentTask
) : ReportingService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultReportingService
    }

    override suspend fun reportContent(eventId: String, score: Int, reason: String) {
        val params = ReportContentTask.Params(roomId, eventId, score, reason)
        reportContentTask.execute(params)
    }
}
