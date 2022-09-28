/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.asLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.query.sum
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.query.SpaceFilter
import org.matrix.android.sdk.api.query.isNormalized
import org.matrix.android.sdk.api.session.room.ResultBoundaries
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.UpdatableLivePageResult
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.spaceSummaryQueryParams
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import org.matrix.android.sdk.api.session.space.SpaceSummaryQueryParams
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.mapper.RoomSummaryMapper
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.findByAlias
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.database.queryIn
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.query.QueryStringValueProcessor
import org.matrix.android.sdk.internal.query.process
import org.matrix.android.sdk.internal.util.mapOptional
import javax.inject.Inject

internal class RoomSummaryDataSource @Inject constructor(
        @SessionDatabase private val realmInstance: RealmInstance,
        private val roomSummaryMapper: RoomSummaryMapper,
        private val queryStringValueProcessor: QueryStringValueProcessor,
) {

    fun getRoomSummary(roomIdOrAlias: String): RoomSummary? {
        val realm = realmInstance.getBlockingRealm()
        return if (roomIdOrAlias.startsWith("!")) {
            // It's a roomId
            RoomSummaryEntity.where(realm, roomId = roomIdOrAlias).first().find()
        } else {
            // Assume it's a room alias
            RoomSummaryEntity.findByAlias(realm, roomIdOrAlias)
        }?.let {
            roomSummaryMapper.map(it)
        }
    }

    fun getRoomSummaryLive(roomId: String): LiveData<Optional<RoomSummary>> {
        return realmInstance.queryFirst {
            RoomSummaryEntity.where(it, roomId).first()
        }.mapOptional {
            roomSummaryMapper.map(it)
        }.asLiveData()
    }

    fun getRoomSummaries(
            queryParams: RoomSummaryQueryParams,
            sortOrder: RoomSortOrder = RoomSortOrder.NONE
    ): List<RoomSummary> {
        val realm = realmInstance.getBlockingRealm()
        return roomSummariesQuery(realm, queryParams).process(sortOrder)
                .find()
                .map {
                    roomSummaryMapper.map(it)
                }
    }

    fun getRoomSummariesLive(
            queryParams: RoomSummaryQueryParams,
            sortOrder: RoomSortOrder = RoomSortOrder.NONE
    ): LiveData<List<RoomSummary>> {
        return realmInstance.queryList(roomSummaryMapper::map) {
            roomSummariesQuery(it, queryParams).process(sortOrder)
        }.asLiveData()
    }

    fun getSpaceSummariesLive(
            queryParams: SpaceSummaryQueryParams,
            sortOrder: RoomSortOrder = RoomSortOrder.NONE
    ): LiveData<List<RoomSummary>> {
        return getRoomSummariesLive(queryParams, sortOrder)
    }

    fun getSpaceSummary(roomIdOrAlias: String): RoomSummary? {
        return getRoomSummary(roomIdOrAlias)
                ?.takeIf { it.roomType == RoomType.SPACE }
    }

    fun getSpaceSummaryLive(roomId: String): LiveData<Optional<RoomSummary>> {
        return realmInstance.queryFirst { realm ->
            RoomSummaryEntity.where(realm, roomId)
                    .query("displayName != ''")
                    .query("roomType == $0", RoomType.SPACE)
                    .first()
        }.mapOptional {
            roomSummaryMapper.map(it)
        }.asLiveData()
    }

    fun getSpaceSummaries(
            spaceSummaryQueryParams: SpaceSummaryQueryParams,
            sortOrder: RoomSortOrder = RoomSortOrder.NONE
    ): List<RoomSummary> {
        return getRoomSummaries(spaceSummaryQueryParams, sortOrder)
    }

    fun getRootSpaceSummaries(): List<RoomSummary> {
        return getRoomSummaries(spaceSummaryQueryParams {
            memberships = listOf(Membership.JOIN)
        })
                .let { allJoinedSpace ->
                    val allFlattenChildren = arrayListOf<RoomSummary>()
                    allJoinedSpace.forEach {
                        flattenSubSpace(it, emptyList(), allFlattenChildren, listOf(Membership.JOIN), false)
                    }
                    val knownNonOrphan = allFlattenChildren.map { it.roomId }.distinct()
                    // keep only root rooms
                    allJoinedSpace.filter { candidate ->
                        !knownNonOrphan.contains(candidate.roomId)
                    }
                }
    }

    fun getBreadcrumbs(queryParams: RoomSummaryQueryParams): List<RoomSummary> {
        val realm = realmInstance.getBlockingRealm()
        return breadcrumbsQuery(realm, queryParams)
                .find()
                .map { roomSummaryMapper.map(it) }
    }

    fun getBreadcrumbsLive(queryParams: RoomSummaryQueryParams): LiveData<List<RoomSummary>> {
        return realmInstance.queryList(roomSummaryMapper::map) { realm ->
            breadcrumbsQuery(realm, queryParams)
        }.asLiveData()
    }

    private fun breadcrumbsQuery(realm: TypedRealm, queryParams: RoomSummaryQueryParams): RealmQuery<RoomSummaryEntity> {
        return roomSummariesQuery(realm, queryParams)
                .query("breadcrumbsIndex > $0", RoomSummary.NOT_IN_BREADCRUMBS)
                .sort("breadcrumbsIndex")
    }

    fun getSortedPagedRoomSummariesLive(
            queryParams: RoomSummaryQueryParams,
            pagedListConfig: PagedList.Config,
            sortOrder: RoomSortOrder
    ): LiveData<PagedList<RoomSummary>> {
        return realmInstance.queryPagedList(pagedListConfig, roomSummaryMapper::map) { realm ->
            roomSummariesQuery(realm, queryParams).process(sortOrder)
        }.asLiveData()
    }

    fun getUpdatablePagedRoomSummariesLive(
            queryParams: RoomSummaryQueryParams,
            pagedListConfig: PagedList.Config,
            sortOrder: RoomSortOrder,
    ): UpdatableLivePageResult {
        val realmDataSourceFactory = monarchy.createDataSourceFactory { realm ->
            roomSummariesQuery(realm, queryParams).process(sortOrder)
        }
        val dataSourceFactory = realmDataSourceFactory.map {
            roomSummaryMapper.map(it)
        }

        val boundaries = MutableLiveData(ResultBoundaries())

        val mapped = monarchy.findAllPagedWithChanges(
                realmDataSourceFactory,
                LivePagedListBuilder(dataSourceFactory, pagedListConfig).also {
                    it.setBoundaryCallback(object : PagedList.BoundaryCallback<RoomSummary>() {
                        override fun onItemAtEndLoaded(itemAtEnd: RoomSummary) {
                            boundaries.postValue(boundaries.value?.copy(frontLoaded = true))
                        }

                        override fun onItemAtFrontLoaded(itemAtFront: RoomSummary) {
                            boundaries.postValue(boundaries.value?.copy(endLoaded = true))
                        }

                        override fun onZeroItemsLoaded() {
                            boundaries.postValue(boundaries.value?.copy(zeroItemLoaded = true))
                        }
                    })
                }
        )

        return object : UpdatableLivePageResult {
            override val livePagedList: LiveData<PagedList<RoomSummary>> = mapped

            override val liveBoundaries: LiveData<ResultBoundaries>
                get() = boundaries

            override var queryParams: RoomSummaryQueryParams = queryParams
                set(value) {
                    field = value
                    realmDataSourceFactory.updateQuery {
                        roomSummariesQuery(it, value).process(sortOrder)
                    }
                }
        }
    }

    fun getCountLive(queryParams: RoomSummaryQueryParams): LiveData<Int> {
        return realmInstance.queryResults {
            roomSummariesQuery(it, queryParams)
        }.map {
            it.list.count()
        }.asLiveData()
    }

    fun getNotificationCountForRooms(queryParams: RoomSummaryQueryParams): RoomAggregateNotificationCount {
        val realm = realmInstance.getBlockingRealm()
        val roomSummariesQuery = roomSummariesQuery(realm, queryParams)
        val notifCount = roomSummariesQuery.sum<Int>("notificationCounts").find()
        val highlightCount = roomSummariesQuery.sum<Int>("highlightCounts").find()
        return RoomAggregateNotificationCount(
                notifCount,
                highlightCount
        )
    }

    private fun roomSummariesQuery(realm: TypedRealm, queryParams: RoomSummaryQueryParams): RealmQuery<RoomSummaryEntity> {
        var query = with(queryStringValueProcessor) {
            RoomSummaryEntity.where(realm)
                    .process(RoomSummaryEntityFields.ROOM_ID, QueryStringValue.IsNotEmpty)
                    .process(queryParams.displayName.toDisplayNameField(), queryParams.displayName)
                    .process(RoomSummaryEntityFields.CANONICAL_ALIAS, queryParams.canonicalAlias)
                    .process(RoomSummaryEntityFields.MEMBERSHIP_STR, queryParams.memberships)
                    .query("isHiddenFromUser == false")
        }

        queryParams.roomTagQueryFilter?.let { tagFilter ->
            tagFilter.isFavorite?.let { fav ->
                query = query.query("isFavourite == $0", fav)
            }
            tagFilter.isLowPriority?.let { lp ->
                query.query("isLowPriority == $0", lp)
            }
            tagFilter.isServerNotice?.let { sn ->
                query = query.query("isServerNotice == $0", sn)
            }
        }

        queryParams.excludeType?.forEach {
            query = query.query("roomType != $0", it)
        }
        queryParams.includeType?.forEach {
            query = query.query("roomType == $0", it)
        }
        query = when (queryParams.roomCategoryFilter) {
            RoomCategoryFilter.ONLY_DM -> query.query("isDirect == true")
            RoomCategoryFilter.ONLY_ROOMS -> query.query("isDirect == false")
            RoomCategoryFilter.ONLY_WITH_NOTIFICATIONS -> query.query("notificationCount > 0")
            null -> query
        }
        // Timber.w("VAL: activeSpaceId : ${queryParams.activeSpaceId}")
        query = when (queryParams.spaceFilter) {
            SpaceFilter.OrphanRooms -> {
                // orphan rooms
                query.query("flattenParentIds.@count == 0")
            }
            is SpaceFilter.ActiveSpace -> {
                // https://github.com/realm/realm-java/issues/5361
                query.query("ANY flattenParentIds == $0", queryParams.spaceFilter.spaceId)
            }
            is SpaceFilter.ExcludeSpace -> {
                query.query("NONE flattenParentIds == $0", queryParams.spaceFilter.spaceId)
            }
            SpaceFilter.NoFilter -> query // nop
        }
        return query
    }

    private fun QueryStringValue.toDisplayNameField(): String {
        return if (isNormalized()) {
            RoomSummaryEntityFields.NORMALIZED_DISPLAY_NAME
        } else {
            RoomSummaryEntityFields.DISPLAY_NAME
        }
    }

    fun getAllRoomSummaryChildOf(spaceAliasOrId: String, memberShips: List<Membership>): List<RoomSummary> {
        val space = getSpaceSummary(spaceAliasOrId) ?: return emptyList()
        val result = ArrayList<RoomSummary>()
        flattenChild(space, emptyList(), result, memberShips)
        return result
    }

    fun getAllRoomSummaryChildOfLive(spaceId: String, memberShips: List<Membership>): LiveData<List<RoomSummary>> {
        // we want to listen to all spaces in hierarchy and on change compute back all childs
        // and switch map to listen those?
        val mediatorLiveData = HierarchyLiveDataHelper(spaceId, memberShips, this).liveData()

        return Transformations.switchMap(mediatorLiveData) { allIds ->
            realmInstance.queryList(roomSummaryMapper::map) {
                it.query(RoomSummaryEntity::class)
                        .queryIn("roomId", allIds)
                        .process("membershipStr", memberShips)
                        .query("isDirect == false")
            }.asLiveData()
        }
    }

    fun getFlattenOrphanRooms(): List<RoomSummary> {
        return getRoomSummaries(
                roomSummaryQueryParams {
                    memberships = Membership.activeMemberships()
                    excludeType = listOf(RoomType.SPACE)
                    roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
                }
        ).filter { isOrphan(it) }
    }

    fun getFlattenOrphanRoomsLive(): LiveData<List<RoomSummary>> {
        return Transformations.map(
                getRoomSummariesLive(roomSummaryQueryParams {
                    memberships = Membership.activeMemberships()
                    excludeType = listOf(RoomType.SPACE)
                    roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
                })
        ) {
            it.filter { isOrphan(it) }
        }
    }

    private fun isOrphan(roomSummary: RoomSummary): Boolean {
        if (roomSummary.roomType == RoomType.SPACE && roomSummary.membership.isActive()) {
            return false
        }
        // all parents line should be orphan
        roomSummary.spaceParents?.forEach { info ->
            if (info.roomSummary != null && !info.roomSummary.membership.isLeft()) {
                if (!isOrphan(info.roomSummary)) {
                    return false
                }
            }
        }

        // it may not have a parent relation but could be a child of some other....
        for (spaceSummary in getSpaceSummaries(spaceSummaryQueryParams { memberships = Membership.activeMemberships() })) {
            if (spaceSummary.spaceChildren?.any { it.childRoomId == roomSummary.roomId } == true) {
                return false
            }
        }

        return true
    }

    fun flattenChild(current: RoomSummary, parenting: List<String>, output: MutableList<RoomSummary>, memberShips: List<Membership>) {
        current.spaceChildren?.sortedBy { it.order ?: it.name }?.forEach { childInfo ->
            if (childInfo.roomType == RoomType.SPACE) {
                // Add recursive
                if (!parenting.contains(childInfo.childRoomId)) { // avoid cycles!
                    getSpaceSummary(childInfo.childRoomId)?.let { subSpace ->
                        if (memberShips.isEmpty() || memberShips.contains(subSpace.membership)) {
                            flattenChild(subSpace, parenting + listOf(current.roomId), output, memberShips)
                        }
                    }
                }
            } else if (childInfo.isKnown) {
                getRoomSummary(childInfo.childRoomId)?.let {
                    if (memberShips.isEmpty() || memberShips.contains(it.membership)) {
                        if (!it.isDirect) {
                            output.add(it)
                        }
                    }
                }
            }
        }
    }

    fun flattenSubSpace(
            current: RoomSummary,
            parenting: List<String>,
            output: MutableList<RoomSummary>,
            memberShips: List<Membership>,
            includeCurrent: Boolean = true
    ) {
        if (includeCurrent) {
            output.add(current)
        }
        current.spaceChildren?.sortedBy { it.order ?: it.name }?.forEach {
            if (it.roomType == RoomType.SPACE) {
                // Add recursive
                if (!parenting.contains(it.childRoomId)) { // avoid cycles!
                    getSpaceSummary(it.childRoomId)?.let { subSpace ->
                        if (memberShips.isEmpty() || memberShips.contains(subSpace.membership)) {
                            output.add(subSpace)
                            flattenSubSpace(subSpace, parenting + listOf(current.roomId), output, memberShips)
                        }
                    }
                }
            }
        }
    }
}
