/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.user.accountdata

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.model.BreadcrumbsEntity
import im.vector.matrix.android.internal.session.sync.model.accountdata.BreadcrumbsContent
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.fetchCopied
import javax.inject.Inject

internal interface UpdateBreadcrumbsTask : Task<UpdateBreadcrumbsTask.Params, Unit> {
    data class Params(
            // Last seen roomId
            val roomId: String
    )
}

internal class DefaultUpdateBreadcrumbsTask @Inject constructor(
        private val saveBreadcrumbsTask: SaveBreadcrumbsTask,
        private val updateUserAccountDataTask: UpdateUserAccountDataTask,
        private val monarchy: Monarchy
) : UpdateBreadcrumbsTask {

    override suspend fun execute(params: UpdateBreadcrumbsTask.Params) {
        // Get the current breadcrumbs in DB
        val bc = monarchy.fetchCopied { realm ->
            // Get the breadcrumbs entity, if any
            realm.where(BreadcrumbsEntity::class.java).findFirst()

        }

        // Modify the list to add the roomId first
        val newRecentRoomIds = if (bc != null) {
            // Ensure the roomId is not already in the list
            bc.recentRoomIds.remove(params.roomId)
            // Add the room at first position
            bc.recentRoomIds.add(0, params.roomId)
            bc.recentRoomIds.toList()
        } else {
            listOf(params.roomId)
        }

        // Update the DB locally, do not wait for the sync
        saveBreadcrumbsTask.execute(SaveBreadcrumbsTask.Params(newRecentRoomIds))

        // And update account data
        updateUserAccountDataTask.execute(UpdateUserAccountDataTask.BreadcrumbsParams(
                breadcrumbsContent = BreadcrumbsContent(
                        newRecentRoomIds
                )
        ))
    }
}
