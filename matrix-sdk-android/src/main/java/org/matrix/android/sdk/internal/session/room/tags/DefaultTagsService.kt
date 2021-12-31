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

package org.matrix.android.sdk.internal.session.room.tags

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.matrix.android.sdk.api.session.room.tags.TagsService

internal class DefaultTagsService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val addTagToRoomTask: AddTagToRoomTask,
        private val deleteTagFromRoomTask: DeleteTagFromRoomTask
) : TagsService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultTagsService
    }

    override suspend fun addTag(tag: String, order: Double?) {
        val params = AddTagToRoomTask.Params(roomId, tag, order)
        addTagToRoomTask.execute(params)
    }

    override suspend fun deleteTag(tag: String) {
        val params = DeleteTagFromRoomTask.Params(roomId, tag)
        deleteTagFromRoomTask.execute(params)
    }
}
