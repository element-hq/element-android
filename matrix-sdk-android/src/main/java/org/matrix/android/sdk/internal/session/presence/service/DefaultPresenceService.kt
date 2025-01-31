/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.presence.service

import org.matrix.android.sdk.api.session.presence.PresenceService
import org.matrix.android.sdk.api.session.presence.model.PresenceEnum
import org.matrix.android.sdk.api.session.presence.model.UserPresence
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.presence.service.task.GetPresenceTask
import org.matrix.android.sdk.internal.session.presence.service.task.SetPresenceTask
import org.matrix.android.sdk.internal.session.sync.SyncPresence
import org.matrix.android.sdk.internal.settings.DefaultLightweightSettingsStorage
import javax.inject.Inject

internal class DefaultPresenceService @Inject constructor(
        @UserId private val userId: String,
        private val setPresenceTask: SetPresenceTask,
        private val getPresenceTask: GetPresenceTask,
        private val lightweightSettingsStorage: DefaultLightweightSettingsStorage
) : PresenceService {

    override suspend fun setMyPresence(presence: PresenceEnum, statusMsg: String?) {
        lightweightSettingsStorage.setSyncPresenceStatus(SyncPresence.from(presence))
        setPresenceTask.execute(SetPresenceTask.Params(userId, presence, statusMsg))
    }

    override suspend fun fetchPresence(userId: String): UserPresence {
        val result = getPresenceTask.execute(GetPresenceTask.Params(userId))

        return UserPresence(
                lastActiveAgo = result.lastActiveAgo,
                statusMessage = result.message,
                isCurrentlyActive = result.isCurrentlyActive,
                presence = result.presence
        )
    }
}
