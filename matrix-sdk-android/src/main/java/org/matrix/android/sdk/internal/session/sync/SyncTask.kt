/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.sync

import io.realm.Realm
import io.realm.RealmConfiguration
import org.greenrobot.eventbus.EventBus
import org.matrix.android.sdk.R
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibilityContent
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.query.findLastForwardChunkOfRoom
import org.matrix.android.sdk.internal.database.query.whereType
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.DefaultInitialSyncProgressService
import org.matrix.android.sdk.internal.session.filter.FilterRepository
import org.matrix.android.sdk.internal.session.homeserver.GetHomeServerCapabilitiesTask
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import org.matrix.android.sdk.internal.session.room.timeline.PaginationTask
import org.matrix.android.sdk.internal.session.sync.model.SyncResponse
import org.matrix.android.sdk.internal.session.user.UserStore
import org.matrix.android.sdk.internal.task.Task
import timber.log.Timber
import javax.inject.Inject

internal interface SyncTask : Task<SyncTask.Params, Unit> {

    data class Params(
            val timeout: Long,
            val presence: SyncPresence?
    )
}

internal class DefaultSyncTask @Inject constructor(
        private val syncAPI: SyncAPI,
        @UserId private val userId: String,
        private val filterRepository: FilterRepository,
        private val syncResponseHandler: SyncResponseHandler,
        private val initialSyncProgressService: DefaultInitialSyncProgressService,
        private val syncTokenStore: SyncTokenStore,
        private val getHomeServerCapabilitiesTask: GetHomeServerCapabilitiesTask,
        private val userStore: UserStore,
        private val syncTaskSequencer: SyncTaskSequencer,
        private val paginationTask: PaginationTask,
        @SessionDatabase
        private val realmConfiguration: RealmConfiguration,
        private val eventBus: EventBus
) : SyncTask {

    override suspend fun execute(params: SyncTask.Params) = syncTaskSequencer.post {
        doSync(params)
    }

    private suspend fun doSync(params: SyncTask.Params) {
        Timber.v("Sync task started on Thread: ${Thread.currentThread().name}")

        val requestParams = HashMap<String, String>()
        var timeout = 0L
        val token = syncTokenStore.getLastToken()
        if (token != null) {
            requestParams["since"] = token
            timeout = params.timeout
        }
        requestParams["timeout"] = timeout.toString()
        requestParams["filter"] = filterRepository.getFilter()
        params.presence?.let { requestParams["set_presence"] = it.value }

        val isInitialSync = token == null
        if (isInitialSync) {
            // We might want to get the user information in parallel too
            userStore.createOrUpdate(userId)
            initialSyncProgressService.endAll()
            initialSyncProgressService.startTask(R.string.initial_sync_start_importing_account, 100)
        }
        // Maybe refresh the home server capabilities data we know
        getHomeServerCapabilitiesTask.execute(Unit)

        val syncResponse = executeRequest<SyncResponse>(eventBus) {
            apiCall = syncAPI.sync(requestParams)
        }
        syncResponseHandler.handleResponse(syncResponse, token)
        if (isInitialSync) {
            initialSyncProgressService.endAll()
            paginateBackwardJoinedRooms(syncResponse)
        }
        Timber.v("Sync task finished on Thread: ${Thread.currentThread().name}")
    }

    private suspend fun paginateBackwardJoinedRooms(syncResponse: SyncResponse) {
        val roomIdsToPaginate = syncResponse.rooms?.join?.keys.orEmpty()
        roomIdsToPaginate.forEach { roomId ->
            val paginate = Realm.getInstance(realmConfiguration).use { realm ->
                val visibilityStateEvent = CurrentStateEventEntity.whereType(realm, roomId, EventType.STATE_ROOM_HISTORY_VISIBILITY).findFirst()
                visibilityStateEvent?.root?.asDomain()?.content?.toModel<RoomHistoryVisibilityContent>()?.historyVisibility != RoomHistoryVisibility.WORLD_READABLE
            }
            if (paginate) {
                repeat(10) { paginateRoom(roomId) }
            }
        }
    }

    private suspend fun paginateRoom(roomId: String) {
        val prevToken = Realm.getInstance(realmConfiguration).use { realm ->
            val liveChunk = ChunkEntity.findLastForwardChunkOfRoom(realm, roomId) ?: return@use null
            if (liveChunk.isLastBackward) {
                return@use null
            }
            return@use liveChunk.prevToken
        } ?: return

        val paginationParams = PaginationTask.Params(
                roomId = roomId,
                from = prevToken,
                direction = PaginationDirection.BACKWARDS,
                limit = 100)
        try {
            paginationTask.execute(paginationParams)
        } catch (failure: Throwable) {
            Timber.v("Failure: $failure")
        }
    }
}
