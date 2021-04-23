/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.summary

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.Sort
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.UpdatableFilterLivePageResult
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.VersioningState
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.database.mapper.RoomSummaryMapper
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.findByAlias
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.query.process
import org.matrix.android.sdk.internal.util.fetchCopyMap
import javax.inject.Inject

internal class RoomSummaryDataSource @Inject constructor(@SessionDatabase private val monarchy: Monarchy,
                                                         private val roomSummaryMapper: RoomSummaryMapper) {

    fun getRoomSummary(roomIdOrAlias: String): RoomSummary? {
        return monarchy
                .fetchCopyMap({
                    if (roomIdOrAlias.startsWith("!")) {
                        // It's a roomId
                        RoomSummaryEntity.where(it, roomId = roomIdOrAlias).findFirst()
                    } else {
                        // Assume it's a room alias
                        RoomSummaryEntity.findByAlias(it, roomIdOrAlias)
                    }
                }, { entity, _ ->
                    roomSummaryMapper.map(entity)
                })
    }

    fun getRoomSummaryLive(roomId: String): LiveData<Optional<RoomSummary>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm -> RoomSummaryEntity.where(realm, roomId).isNotEmpty(RoomSummaryEntityFields.DISPLAY_NAME) },
                { roomSummaryMapper.map(it) }
        )
        return Transformations.map(liveData) { results ->
            results.firstOrNull().toOptional()
        }
    }

    fun getRoomSummaries(queryParams: RoomSummaryQueryParams): List<RoomSummary> {
        return monarchy.fetchAllMappedSync(
                { roomSummariesQuery(it, queryParams) },
                { roomSummaryMapper.map(it) }
        )
    }

    fun getRoomSummariesLive(queryParams: RoomSummaryQueryParams): LiveData<List<RoomSummary>> {
        return monarchy.findAllMappedWithChanges(
                { roomSummariesQuery(it, queryParams) },
                { roomSummaryMapper.map(it) }
        )
    }

    fun getBreadcrumbs(queryParams: RoomSummaryQueryParams): List<RoomSummary> {
        return monarchy.fetchAllMappedSync(
                { breadcrumbsQuery(it, queryParams) },
                { roomSummaryMapper.map(it) }
        )
    }

    fun getBreadcrumbsLive(queryParams: RoomSummaryQueryParams): LiveData<List<RoomSummary>> {
        return monarchy.findAllMappedWithChanges(
                { breadcrumbsQuery(it, queryParams) },
                { roomSummaryMapper.map(it) }
        )
    }

    private fun breadcrumbsQuery(realm: Realm, queryParams: RoomSummaryQueryParams): RealmQuery<RoomSummaryEntity> {
        return roomSummariesQuery(realm, queryParams)
                .greaterThan(RoomSummaryEntityFields.BREADCRUMBS_INDEX, RoomSummary.NOT_IN_BREADCRUMBS)
                .sort(RoomSummaryEntityFields.BREADCRUMBS_INDEX)
    }

    fun getSortedPagedRoomSummariesLive(queryParams: RoomSummaryQueryParams,
                                        pagedListConfig: PagedList.Config): LiveData<PagedList<RoomSummary>> {
        val realmDataSourceFactory = monarchy.createDataSourceFactory { realm ->
            roomSummariesQuery(realm, queryParams)
                    .sort(RoomSummaryEntityFields.LAST_ACTIVITY_TIME, Sort.DESCENDING)
        }
        val dataSourceFactory = realmDataSourceFactory.map {
            roomSummaryMapper.map(it)
        }
        return monarchy.findAllPagedWithChanges(
                realmDataSourceFactory,
                LivePagedListBuilder(dataSourceFactory, pagedListConfig)
        )
    }

    fun getFilteredPagedRoomSummariesLive(queryParams: RoomSummaryQueryParams,
                                          pagedListConfig: PagedList.Config): UpdatableFilterLivePageResult {
        val realmDataSourceFactory = monarchy.createDataSourceFactory { realm ->
            roomSummariesQuery(realm, queryParams)
                    .sort(RoomSummaryEntityFields.LAST_ACTIVITY_TIME, Sort.DESCENDING)
        }
        val dataSourceFactory = realmDataSourceFactory.map {
            roomSummaryMapper.map(it)
        }

        val mapped = monarchy.findAllPagedWithChanges(
                realmDataSourceFactory,
                LivePagedListBuilder(dataSourceFactory, pagedListConfig)
        )

        return object : UpdatableFilterLivePageResult {
            override val livePagedList: LiveData<PagedList<RoomSummary>> = mapped

            override fun updateQuery(queryParams: RoomSummaryQueryParams) {
                realmDataSourceFactory.updateQuery {
                    roomSummariesQuery(it, queryParams)
                            .sort(RoomSummaryEntityFields.LAST_ACTIVITY_TIME, Sort.DESCENDING)
                }
            }
        }
    }

    fun getNotificationCountForRooms(queryParams: RoomSummaryQueryParams): RoomAggregateNotificationCount {
        var notificationCount: RoomAggregateNotificationCount? = null
        monarchy.doWithRealm { realm ->
            val roomSummariesQuery = roomSummariesQuery(realm, queryParams)
            val notifCount = roomSummariesQuery.sum(RoomSummaryEntityFields.NOTIFICATION_COUNT).toInt()
            val highlightCount = roomSummariesQuery.sum(RoomSummaryEntityFields.HIGHLIGHT_COUNT).toInt()
            notificationCount = RoomAggregateNotificationCount(
                    notifCount,
                    highlightCount
            )
        }
        return notificationCount!!
    }

    private fun roomSummariesQuery(realm: Realm, queryParams: RoomSummaryQueryParams): RealmQuery<RoomSummaryEntity> {
        val query = RoomSummaryEntity.where(realm)
        query.process(RoomSummaryEntityFields.ROOM_ID, queryParams.roomId)
        query.process(RoomSummaryEntityFields.DISPLAY_NAME, queryParams.displayName)
        query.process(RoomSummaryEntityFields.CANONICAL_ALIAS, queryParams.canonicalAlias)
        query.process(RoomSummaryEntityFields.MEMBERSHIP_STR, queryParams.memberships)
        query.notEqualTo(RoomSummaryEntityFields.VERSIONING_STATE_STR, VersioningState.UPGRADED_ROOM_JOINED.name)

        queryParams.roomCategoryFilter?.let {
            when (it) {
                RoomCategoryFilter.ONLY_DM                 -> query.equalTo(RoomSummaryEntityFields.IS_DIRECT, true)
                RoomCategoryFilter.ONLY_ROOMS              -> query.equalTo(RoomSummaryEntityFields.IS_DIRECT, false)
                RoomCategoryFilter.ONLY_WITH_NOTIFICATIONS -> query.greaterThan(RoomSummaryEntityFields.NOTIFICATION_COUNT, 0)
                RoomCategoryFilter.ALL                     -> {
                    // nop
                }
            }
        }
        queryParams.roomTagQueryFilter?.let {
            it.isFavorite?.let { fav ->
                query.equalTo(RoomSummaryEntityFields.IS_FAVOURITE, fav)
            }
            it.isLowPriority?.let { lp ->
                query.equalTo(RoomSummaryEntityFields.IS_LOW_PRIORITY, lp)
            }
            it.isServerNotice?.let { sn ->
                query.equalTo(RoomSummaryEntityFields.IS_SERVER_NOTICE, sn)
            }
        }
        return query
    }
}
