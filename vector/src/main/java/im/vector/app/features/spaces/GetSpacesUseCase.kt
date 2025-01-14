/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces

import im.vector.app.core.di.ActiveSessionHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.space.SpaceSummaryQueryParams
import org.matrix.android.sdk.flow.flow
import javax.inject.Inject

class GetSpacesUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {

    fun execute(params: SpaceSummaryQueryParams): Flow<List<RoomSummary>> {
        val session = activeSessionHolder.getSafeActiveSession()

        return session?.flow()?.liveSpaceSummaries(params) ?: emptyFlow()
    }
}
