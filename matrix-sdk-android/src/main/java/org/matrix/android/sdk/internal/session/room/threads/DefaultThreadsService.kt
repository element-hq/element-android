/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.threads

import androidx.lifecycle.LiveData
import com.zhuinden.monarchy.Monarchy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.realm.Realm
import org.matrix.android.sdk.api.session.room.threads.ThreadsService
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummary
import org.matrix.android.sdk.internal.database.helper.enhanceWithEditions
import org.matrix.android.sdk.internal.database.helper.findAllThreadsForRoomId
import org.matrix.android.sdk.internal.database.mapper.ThreadSummaryMapper
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntity
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.room.relation.threads.FetchThreadSummariesTask
import org.matrix.android.sdk.internal.session.room.relation.threads.FetchThreadTimelineTask

internal class DefaultThreadsService @AssistedInject constructor(
        @Assisted private val roomId: String,
        @UserId private val userId: String,
        private val fetchThreadTimelineTask: FetchThreadTimelineTask,
        private val fetchThreadSummariesTask: FetchThreadSummariesTask,
        @SessionDatabase private val monarchy: Monarchy,
        private val timelineEventMapper: TimelineEventMapper,
        private val threadSummaryMapper: ThreadSummaryMapper
) : ThreadsService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultThreadsService
    }

    override fun getAllThreadSummariesLive(): LiveData<List<ThreadSummary>> {
        return monarchy.findAllMappedWithChanges(
                { ThreadSummaryEntity.findAllThreadsForRoomId(it, roomId = roomId) },
                {
                    threadSummaryMapper.map(it)
                }
        )
    }

    override fun getAllThreadSummaries(): List<ThreadSummary> {
        return monarchy.fetchAllMappedSync(
                { ThreadSummaryEntity.findAllThreadsForRoomId(it, roomId = roomId) },
                { threadSummaryMapper.map(it) }
        )
    }

    override fun enhanceThreadWithEditions(threads: List<ThreadSummary>): List<ThreadSummary> {
        return Realm.getInstance(monarchy.realmConfiguration).use {
            threads.enhanceWithEditions(it, roomId)
        }
    }

    override suspend fun fetchThreadTimeline(rootThreadEventId: String, from: String, limit: Int) {
        fetchThreadTimelineTask.execute(
                FetchThreadTimelineTask.Params(
                        roomId = roomId,
                        rootThreadEventId = rootThreadEventId,
                        from = from,
                        limit = limit
                )
        )
    }

    override suspend fun fetchThreadSummaries() {
        fetchThreadSummariesTask.execute(
                FetchThreadSummariesTask.Params(
                        roomId = roomId
                )
        )
    }
}
