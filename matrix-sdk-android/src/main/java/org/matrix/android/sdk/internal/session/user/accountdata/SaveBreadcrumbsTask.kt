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
package org.matrix.android.sdk.internal.session.user.accountdata

import com.zhuinden.monarchy.Monarchy
import io.realm.RealmList
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.internal.database.model.BreadcrumbsEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import javax.inject.Inject

/**
 * Save the Breadcrumbs roomId list in DB, either from the sync, or updated locally
 */
internal interface SaveBreadcrumbsTask : Task<SaveBreadcrumbsTask.Params, Unit> {
    data class Params(
            val recentRoomIds: List<String>
    )
}

internal class DefaultSaveBreadcrumbsTask @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy
) : SaveBreadcrumbsTask {

    override suspend fun execute(params: SaveBreadcrumbsTask.Params) {
        monarchy.awaitTransaction { realm ->
            // Get or create a breadcrumbs entity
            val entity = BreadcrumbsEntity.getOrCreate(realm)

            // And save the new received list
            entity.recentRoomIds = RealmList<String>().apply { addAll(params.recentRoomIds) }

            // Update the room summaries
            // Reset all the indexes...
            RoomSummaryEntity.where(realm)
                    .greaterThan(RoomSummaryEntityFields.BREADCRUMBS_INDEX, RoomSummary.NOT_IN_BREADCRUMBS)
                    .findAll()
                    .forEach {
                        it.breadcrumbsIndex = RoomSummary.NOT_IN_BREADCRUMBS
                    }

            // ...and apply new indexes
            params.recentRoomIds.forEachIndexed { index, roomId ->
                RoomSummaryEntity.where(realm, roomId)
                        .findFirst()
                        ?.breadcrumbsIndex = index
            }
        }
    }
}
