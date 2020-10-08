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

package org.matrix.android.sdk.internal.session.room.draft

import androidx.lifecycle.LiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.room.send.DraftService
import org.matrix.android.sdk.api.session.room.send.UserDraft
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.launchToCallback
import org.matrix.android.sdk.internal.util.MatrixCoroutineDispatchers

internal class DefaultDraftService @AssistedInject constructor(@Assisted private val roomId: String,
                                                               private val draftRepository: DraftRepository,
                                                               private val taskExecutor: TaskExecutor,
                                                               private val coroutineDispatchers: MatrixCoroutineDispatchers
) : DraftService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): DraftService
    }

    /**
     * The draft stack can contain several drafts. Depending of the draft to save, it will update the top draft, or create a new draft,
     * or even move an existing draft to the top of the list
     */
    override fun saveDraft(draft: UserDraft, callback: MatrixCallback<Unit>): Cancelable {
        return taskExecutor.executorScope.launchToCallback(coroutineDispatchers.main, callback) {
            draftRepository.saveDraft(roomId, draft)
        }
    }

    override fun deleteDraft(callback: MatrixCallback<Unit>): Cancelable {
        return taskExecutor.executorScope.launchToCallback(coroutineDispatchers.main, callback) {
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
