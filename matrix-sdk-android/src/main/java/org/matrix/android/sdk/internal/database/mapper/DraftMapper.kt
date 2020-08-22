/*
 * Copyright 2019 New Vector Ltd
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

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.session.room.send.UserDraft
import org.matrix.android.sdk.internal.database.model.DraftEntity

/**
 * DraftEntity <-> UserDraft
 */
internal object DraftMapper {

    fun map(entity: DraftEntity): UserDraft {
        return when (entity.draftMode) {
            DraftEntity.MODE_REGULAR -> UserDraft.REGULAR(entity.content)
            DraftEntity.MODE_EDIT    -> UserDraft.EDIT(entity.linkedEventId, entity.content)
            DraftEntity.MODE_QUOTE   -> UserDraft.QUOTE(entity.linkedEventId, entity.content)
            DraftEntity.MODE_REPLY   -> UserDraft.REPLY(entity.linkedEventId, entity.content)
            else                     -> null
        } ?: UserDraft.REGULAR("")
    }

    fun map(domain: UserDraft): DraftEntity {
        return when (domain) {
            is UserDraft.REGULAR -> DraftEntity(content = domain.text, draftMode = DraftEntity.MODE_REGULAR, linkedEventId = "")
            is UserDraft.EDIT    -> DraftEntity(content = domain.text, draftMode = DraftEntity.MODE_EDIT, linkedEventId = domain.linkedEventId)
            is UserDraft.QUOTE   -> DraftEntity(content = domain.text, draftMode = DraftEntity.MODE_QUOTE, linkedEventId = domain.linkedEventId)
            is UserDraft.REPLY   -> DraftEntity(content = domain.text, draftMode = DraftEntity.MODE_REPLY, linkedEventId = domain.linkedEventId)
        }
    }
}
