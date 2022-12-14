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

package org.matrix.android.sdk.internal.session.room.threads

import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.where
import org.matrix.android.sdk.api.session.room.ResultBoundaries
import org.matrix.android.sdk.api.session.room.threads.FetchThreadsResult
import org.matrix.android.sdk.api.session.room.threads.ThreadFilter
import org.matrix.android.sdk.api.session.room.threads.ThreadLivePageResult
import org.matrix.android.sdk.api.session.room.threads.ThreadsService
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummary
import org.matrix.android.sdk.internal.database.helper.enhanceWithEditions
import org.matrix.android.sdk.internal.database.helper.findAllThreadsForRoomId
import org.matrix.android.sdk.internal.database.mapper.ThreadSummaryMapper
import org.matrix.android.sdk.internal.database.model.threads.ThreadListPageEntity
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntity
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntityFields
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.relation.threads.FetchThreadSummariesTask
import org.matrix.android.sdk.internal.session.room.relation.threads.FetchThreadTimelineTask
import org.matrix.android.sdk.internal.util.awaitTransaction

internal class DefaultThreadsService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val fetchThreadTimelineTask: FetchThreadTimelineTask,
        @SessionDatabase private val monarchy: Monarchy,
        private val threadSummaryMapper: ThreadSummaryMapper,
        private val fetchThreadSummariesTask: FetchThreadSummariesTask,
) : ThreadsService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultThreadsService
    }

    override suspend fun getPagedThreadsList(userParticipating: Boolean, pagedListConfig: PagedList.Config): ThreadLivePageResult {
        monarchy.awaitTransaction { realm ->
            realm.where<ThreadListPageEntity>().findAll().deleteAllFromRealm()
        }

        val realmDataSourceFactory = monarchy.createDataSourceFactory { realm ->
            realm
                    .where<ThreadSummaryEntity>().equalTo(ThreadSummaryEntityFields.PAGE.ROOM_ID, roomId)
                    .sort(ThreadSummaryEntityFields.LATEST_THREAD_EVENT_ENTITY.ORIGIN_SERVER_TS, Sort.DESCENDING)
        }

        val dataSourceFactory = realmDataSourceFactory.map {
            threadSummaryMapper.map(it)
        }

        val boundaries = MutableLiveData(ResultBoundaries())

        val builder = LivePagedListBuilder(dataSourceFactory, pagedListConfig).also {
            it.setBoundaryCallback(object : PagedList.BoundaryCallback<ThreadSummary>() {
                override fun onItemAtEndLoaded(itemAtEnd: ThreadSummary) {
                    boundaries.postValue(boundaries.value?.copy(endLoaded = true))
                }

                override fun onItemAtFrontLoaded(itemAtFront: ThreadSummary) {
                    boundaries.postValue(boundaries.value?.copy(frontLoaded = true))
                }

                override fun onZeroItemsLoaded() {
                    boundaries.postValue(boundaries.value?.copy(zeroItemLoaded = true))
                }
            })
        }

        val livePagedList = monarchy.findAllPagedWithChanges(
                realmDataSourceFactory,
                builder
        )
        return ThreadLivePageResult(livePagedList, boundaries)
    }

    override suspend fun fetchThreadList(nextBatchId: String?, limit: Int, filter: ThreadFilter): FetchThreadsResult {
        return fetchThreadSummariesTask.execute(
                FetchThreadSummariesTask.Params(
                        roomId = roomId,
                        from = nextBatchId,
                        limit = limit,
                        filter = filter
                )
        )
    }

    override suspend fun getAllThreadSummaries(): List<ThreadSummary> {
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
}
