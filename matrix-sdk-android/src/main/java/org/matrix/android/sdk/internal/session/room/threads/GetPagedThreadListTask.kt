/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.threads

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.ResultBoundaries
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummary
import org.matrix.android.sdk.internal.database.mapper.ThreadSummaryMapper
import org.matrix.android.sdk.internal.database.model.threads.ThreadListPageEntity
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntity
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntityFields
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.relation.threads.FetchThreadSummariesTask
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction

internal interface GetPagedThreadListTask : Task<GetPagedThreadListTask.Params, LiveData<PagedList<ThreadSummary>>> {
    data class Params(val roomId: String, val coroutineScope: CoroutineScope, val isUserParticipating: Boolean)
}

internal class DefaultGetPagedThreadListTask(
        @SessionDatabase private val monarchy: Monarchy,
        private val threadSummaryMapper: ThreadSummaryMapper,
        private val fetchThreadSummariesTask: FetchThreadSummariesTask
) : GetPagedThreadListTask {

    private var roomId: String? = null
    private var coroutineScope: CoroutineScope? = null
    private var isUserParticipating: Boolean = true

    private var nextBatchId: String? = null
    private var hasReachedEnd: Boolean = false

    private val defaultPagedListConfig
        get() = PagedList.Config.Builder()
                .setPageSize(10)
                .setInitialLoadSizeHint(10)
                .setEnablePlaceholders(false)
                .setPrefetchDistance(5)
                .build()

    override suspend fun execute(params: GetPagedThreadListTask.Params): LiveData<PagedList<ThreadSummary>> {
        coroutineScope = params.coroutineScope
        roomId = params.roomId
        nextBatchId = null
        hasReachedEnd = false
        isUserParticipating = params.isUserParticipating

        monarchy.awaitTransaction { realm ->
            realm.where<ThreadListPageEntity>().findAll().deleteAllFromRealm()
        }

        val realmDataSourceFactory = monarchy.createDataSourceFactory { realm ->
            realm
                    .where<ThreadSummaryEntity>().equalTo(ThreadSummaryEntityFields.PAGE.ROOM_ID, roomId)
                    .sort(ThreadSummaryEntityFields.LATEST_THREAD_EVENT_ENTITY.ORIGIN_SERVER_TS, Sort.DESCENDING)
        }

        fetchNextPage()

        return monarchy.findAllPagedWithChanges(
                realmDataSourceFactory,
                getListBuilder(realmDataSourceFactory)
        )
    }

    private suspend fun getListBuilder(realmDataSourceFactory: Monarchy.RealmDataSourceFactory<ThreadSummaryEntity>): LivePagedListBuilder<Int, ThreadSummary> {
        val dataSourceFactory = realmDataSourceFactory.map {
            threadSummaryMapper.map(it)
        }

        val boundaries = MutableLiveData(ResultBoundaries())

        val builder = LivePagedListBuilder(dataSourceFactory, defaultPagedListConfig).also {
            it.setBoundaryCallback(object : PagedList.BoundaryCallback<ThreadSummary>() {
                override fun onItemAtEndLoaded(itemAtEnd: ThreadSummary) {
                    boundaries.postValue(boundaries.value?.copy(endLoaded = true))
                    coroutineScope?.launch {
                        if (!hasReachedEnd) {
                            fetchNextPage()
                        }
                    }
                }

                override fun onItemAtFrontLoaded(itemAtFront: ThreadSummary) {
                    boundaries.postValue(boundaries.value?.copy(frontLoaded = true))
                }

                override fun onZeroItemsLoaded() {
                    boundaries.postValue(boundaries.value?.copy(zeroItemLoaded = true))
                }
            })
        }
        return builder
    }

    private suspend fun fetchNextPage() {
        roomId?.let { roomId ->
            fetchThreadSummariesTask.execute(
                    FetchThreadSummariesTask.Params(
                            roomId = roomId,
                            from = nextBatchId,
                            limit = defaultPagedListConfig.pageSize,
                            isUserParticipating = isUserParticipating
                    )
            ).also { result ->
                when (result) {
                    is FetchThreadSummariesTask.FetchThreadsResult.ReachedEnd -> {
                        hasReachedEnd = true
                    }
                    is FetchThreadSummariesTask.FetchThreadsResult.ShouldFetchMore -> {
                        nextBatchId = result.nextBatch
                    }
                    else -> {
                    }
                }
            }
        }
    }
}
