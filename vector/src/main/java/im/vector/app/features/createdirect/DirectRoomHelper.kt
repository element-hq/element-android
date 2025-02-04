/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.createdirect

import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.plan.CreatedRoom
import im.vector.app.features.raw.wellknown.getElementWellknown
import im.vector.app.features.raw.wellknown.isE2EByDefault
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import javax.inject.Inject

class DirectRoomHelper @Inject constructor(
        private val rawService: RawService,
        private val session: Session,
        private val analyticsTracker: AnalyticsTracker,
        private val vectorPreferences: VectorPreferences,
) {

    suspend fun ensureDMExists(userId: String): String {
        val existingRoomId = tryOrNull { session.roomService().getExistingDirectRoomWithUser(userId) }
        val roomId: String
        if (existingRoomId != null) {
            roomId = existingRoomId
        } else {
            val adminE2EByDefault = rawService.getElementWellknown(session.sessionParams)
                    ?.isE2EByDefault()
                    ?: true

            val roomParams = CreateRoomParams().apply {
                invitedUserIds.add(userId)
                setDirectMessage()
                enableEncryptionIfInvitedUsersSupportIt = adminE2EByDefault
            }
            roomId = if (vectorPreferences.isDeferredDmEnabled()) {
                session.roomService().createLocalRoom(roomParams)
            } else {
                analyticsTracker.capture(CreatedRoom(isDM = roomParams.isDirect.orFalse()))
                session.roomService().createRoom(roomParams)
            }
        }
        return roomId
    }
}
