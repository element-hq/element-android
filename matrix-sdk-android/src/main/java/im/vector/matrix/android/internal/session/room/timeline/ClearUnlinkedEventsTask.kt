/*

  * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.internal.session.room.timeline

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.helper.deleteOnCascade
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.ChunkEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.awaitTransaction
import javax.inject.Inject

internal interface ClearUnlinkedEventsTask : Task<ClearUnlinkedEventsTask.Params, Unit> {

    data class Params(val roomId: String)
}

internal class DefaultClearUnlinkedEventsTask @Inject constructor() : ClearUnlinkedEventsTask {

    override suspend fun execute(params: ClearUnlinkedEventsTask.Params) {
        return
        /*monarchy.awaitTransaction { localRealm ->
            val unlinkedChunks = ChunkEntity
                    .where(localRealm, roomId = params.roomId)
                    .findAll()
            unlinkedChunks.forEach {
                it.deleteOnCascade()
            }
        }
        */
    }
}
