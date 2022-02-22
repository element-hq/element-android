/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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
 */

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummary
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntity
import javax.inject.Inject

internal class ThreadSummaryMapper @Inject constructor() {

    fun map(threadSummary: ThreadSummaryEntity): ThreadSummary {
        return ThreadSummary(
                roomId = threadSummary.room?.firstOrNull()?.roomId.orEmpty(),
                rootEvent = threadSummary.rootThreadEventEntity?.asDomain(),
                latestEvent = threadSummary.latestThreadEventEntity?.asDomain(),
                rootEventId = threadSummary.rootThreadEventId.orEmpty(),
                rootThreadSenderInfo = SenderInfo(
                        userId = threadSummary.rootThreadEventEntity?.sender ?: "",
                        displayName = threadSummary.rootThreadSenderName,
                        isUniqueDisplayName = threadSummary.rootThreadIsUniqueDisplayName,
                        avatarUrl = threadSummary.rootThreadSenderAvatar
                ),
                latestThreadSenderInfo = SenderInfo(
                        userId = threadSummary.latestThreadEventEntity?.sender ?: "",
                        displayName = threadSummary.latestThreadSenderName,
                        isUniqueDisplayName = threadSummary.latestThreadIsUniqueDisplayName,
                        avatarUrl = threadSummary.latestThreadSenderAvatar
                ),
                isUserParticipating = threadSummary.isUserParticipating,
                numberOfThreads = threadSummary.numberOfThreads
        )
    }
}
