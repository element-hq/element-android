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

package org.matrix.android.sdk.internal.session.space

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.RealmQuery
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.model.VersioningState
import org.matrix.android.sdk.api.session.space.SpaceSummary
import org.matrix.android.sdk.api.session.space.SpaceSummaryQueryParams
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.database.mapper.SpaceSummaryMapper
import org.matrix.android.sdk.internal.database.model.SpaceSummaryEntity
import org.matrix.android.sdk.internal.database.model.SpaceSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.findByAlias
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.query.process
import org.matrix.android.sdk.internal.util.fetchCopyMap
import javax.inject.Inject

internal class SpaceSummaryDataSource @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        private val spaceSummaryMapper: SpaceSummaryMapper
) {

    fun getSpaceSummary(roomIdOrAlias: String): SpaceSummary? {
        return monarchy
                .fetchCopyMap({
                    if (roomIdOrAlias.startsWith("!")) {
                        // It's a roomId
                        SpaceSummaryEntity.where(it, roomId = roomIdOrAlias).findFirst()
                    } else {
                        // Assume it's a room alias
                        SpaceSummaryEntity.findByAlias(it, roomIdOrAlias)
                    }
                }, { entity, _ ->
                    spaceSummaryMapper.map(entity)
                })
    }

    fun getSpaceSummaryLive(roomId: String): LiveData<Optional<SpaceSummary>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm -> SpaceSummaryEntity.where(realm, roomId).isNotEmpty(SpaceSummaryEntityFields.ROOM_SUMMARY_ENTITY.DISPLAY_NAME) },
                { spaceSummaryMapper.map(it) }
        )
        return Transformations.map(liveData) { results ->
            results.firstOrNull().toOptional()
        }
    }

    fun getSpaceSummaries(queryParams: SpaceSummaryQueryParams): List<SpaceSummary> {
        return monarchy.fetchAllMappedSync(
                { spaceSummariesQuery(it, queryParams) },
                { spaceSummaryMapper.map(it) }
        )
    }

    fun getRoomSummariesLive(queryParams: RoomSummaryQueryParams): LiveData<List<SpaceSummary>> {
        return monarchy.findAllMappedWithChanges(
                { spaceSummariesQuery(it, queryParams) },
                { spaceSummaryMapper.map(it) }
        )
    }

    private fun spaceSummariesQuery(realm: Realm, queryParams: SpaceSummaryQueryParams): RealmQuery<SpaceSummaryEntity> {
        val query = SpaceSummaryEntity.where(realm)
        query.process(SpaceSummaryEntityFields.SPACE_ID, queryParams.roomId)
        query.process(SpaceSummaryEntityFields.ROOM_SUMMARY_ENTITY.DISPLAY_NAME, queryParams.displayName)
        query.process(SpaceSummaryEntityFields.ROOM_SUMMARY_ENTITY.CANONICAL_ALIAS, queryParams.canonicalAlias)
        query.process(SpaceSummaryEntityFields.ROOM_SUMMARY_ENTITY.MEMBERSHIP_STR, queryParams.memberships)
        query.notEqualTo(SpaceSummaryEntityFields.ROOM_SUMMARY_ENTITY.VERSIONING_STATE_STR, VersioningState.UPGRADED_ROOM_JOINED.name)
        return query
    }
}
