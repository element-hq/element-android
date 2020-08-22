/*
 * Copyright 2019 New Vector Ltd
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

package org.matrix.android.sdk.api.session.room.send

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.util.Cancelable

interface DraftService {

    /**
     * Save or update a draft to the room
     */
    fun saveDraft(draft: UserDraft, callback: MatrixCallback<Unit>): Cancelable

    /**
     * Delete the last draft, basically just after sending the message
     */
    fun deleteDraft(callback: MatrixCallback<Unit>): Cancelable

    /**
     * Return the current drafts if any, as a live data
     * The draft list can contain one draft for {regular, reply, quote} and an arbitrary number of {edit} drafts
     */
    fun getDraftsLive(): LiveData<List<UserDraft>>
}
