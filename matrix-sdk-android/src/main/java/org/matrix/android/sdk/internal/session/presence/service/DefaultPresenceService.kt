/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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
