/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.draft

import androidx.lifecycle.LiveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.session.room.send.DraftService
import org.matrix.android.sdk.api.session.room.send.UserDraft
import org.matrix.android.sdk.api.util.Optional

internal class DefaultDraftService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val draftRepository: DraftRepository,
        private val coroutineDispatchers: MatrixCoroutineDispatchers
) : DraftService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultDraftService
    }

    /**
     * The draft stack can contain several drafts. Depending of the draft to save, it will update the top draft, or create a new draft,
     * or even move an existing draft to the top of the list
     */
    override suspend fun saveDraft(draft: UserDraft) {
        withContext(coroutineDispatchers.main) {
            draftRepository.saveDraft(roomId, draft)
        }
    }

    override suspend fun deleteDraft() {
        withContext(coroutineDispatchers.main) {
            draftRepository.deleteDraft(roomId)
        }
    }

    override fun getDraft(): UserDraft? {
        return draftRepository.getDraft(roomId)
    }

    override fun getDraftLive(): LiveData<Optional<UserDraft>> {
        return draftRepository.getDraftsLive(roomId)
    }
}
