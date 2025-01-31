/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
