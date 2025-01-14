/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live

import im.vector.app.core.di.ActiveSessionHolder
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.location.UpdateLiveLocationShareResult
import javax.inject.Inject

class StopLiveLocationShareUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder
) {

    suspend fun execute(roomId: String): UpdateLiveLocationShareResult? {
        return sendStoppedBeaconInfo(roomId)
    }

    private suspend fun sendStoppedBeaconInfo(roomId: String): UpdateLiveLocationShareResult? {
        return activeSessionHolder.getActiveSession()
                .getRoom(roomId)
                ?.locationSharingService()
                ?.stopLiveLocationShare()
    }
}
