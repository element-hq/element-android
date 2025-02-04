/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live.map

import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapLatest
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import javax.inject.Inject

class GetListOfUserLiveLocationUseCase @Inject constructor(
        private val session: Session,
        private val userLiveLocationViewStateMapper: UserLiveLocationViewStateMapper,
) {

    fun execute(roomId: String): Flow<List<UserLiveLocationViewState>> {
        return session.getRoom(roomId)
                ?.locationSharingService()
                ?.getRunningLiveLocationShareSummaries()
                ?.asFlow()
                ?.mapLatest { it.mapNotNull { summary -> userLiveLocationViewStateMapper.map(summary) } }
                ?: emptyFlow()
    }
}
