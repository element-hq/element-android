/*
 * Copyright (c) 2020 New Vector Ltd
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
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.VersioningState
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
import io.realm.Realm
import io.realm.RealmQuery
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

    private fun roomSummariesQuery(realm: Realm, queryParams: RoomSummaryQueryParams): RealmQuery<RoomSummaryEntity> {
        val query = RoomSummaryEntity.where(realm)
        query.process(RoomSummaryEntityFields.ROOM_ID, queryParams.roomId)
        query.process(RoomSummaryEntityFields.DISPLAY_NAME, queryParams.displayName)
        query.process(RoomSummaryEntityFields.CANONICAL_ALIAS, queryParams.canonicalAlias)
        query.process(RoomSummaryEntityFields.MEMBERSHIP_STR, queryParams.memberships)
        query.notEqualTo(RoomSummaryEntityFields.VERSIONING_STATE_STR, VersioningState.UPGRADED_ROOM_JOINED.name)
        return query
    }
}
