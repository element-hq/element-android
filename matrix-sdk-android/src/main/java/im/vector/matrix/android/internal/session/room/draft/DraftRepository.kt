/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.room.draft

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import im.vector.matrix.android.BuildConfig
import im.vector.matrix.android.api.session.room.send.UserDraft
import im.vector.matrix.android.internal.database.awaitTransaction
import im.vector.matrix.android.internal.database.mapper.DraftMapper
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.DraftMode
import im.vector.matrix.sqldelight.session.DraftQueries
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

internal class DraftRepository @Inject constructor(private val sessionDatabase: SessionDatabase,
                                                   private val draftMapper: DraftMapper,
                                                   private val coroutineDispatchers: MatrixCoroutineDispatchers) {

    suspend fun saveDraft(roomId: String, userDraft: UserDraft) {
        sessionDatabase.awaitTransaction(coroutineDispatchers) {
            saveDraft(userDraft, roomId)
        }
    }

    suspend fun deleteDraft(roomId: String) {
        sessionDatabase.awaitTransaction(coroutineDispatchers) {
            it.draftQueries.deleteTopDraftFromRoom(roomId)
        }
    }

    private fun saveDraft(draft: UserDraft, roomId: String) {
        // Save only valid draft
        if (draft.isValid()) {
            // Add a new draft or update the current one?

            // Is it an update of the top draft?
            val topDraft = sessionDatabase.draftQueries.getTopDraftFromRoom(roomId).executeAsOneOrNull()
            if (topDraft == null) {
                Timber.d("Draft: create a new draft ${privacySafe(draft)}")
                sessionDatabase.draftQueries.insertDraft(roomId, draft)
            } else if (topDraft.draft_mode == DraftMode.MODE_EDIT) {
                // top draft is an edit
                if (draft is UserDraft.EDIT) {
                    if (topDraft.linked_event_id == draft.linkedEventId) {
                        // Update the top draft
                        Timber.d("Draft: update the top edit draft ${privacySafe(draft)}")
                        sessionDatabase.draftQueries.updateTopDraft(content = draft.text, linkedEventId = topDraft.linked_event_id, draftMode = topDraft.draft_mode, roomId = roomId)
                    } else {
                        // Check a previously EDIT draft with the same id
                        val existingEditDraftOfSameEvent = sessionDatabase.draftQueries
                                .getLocalId(roomId, DraftMode.MODE_EDIT, draft.linkedEventId)
                                .executeAsOneOrNull()

                        if (existingEditDraftOfSameEvent != null) {
                            sessionDatabase.draftQueries.setDraftToTop(existingEditDraftOfSameEvent)
                            // Ignore the new text, restore what was typed before, by putting the draft to the top
                        } else {
                            Timber.d("Draft: add a new edit draft ${privacySafe(draft)}")
                            sessionDatabase.draftQueries.insertDraft(roomId, draft)
                        }
                    }
                } else {
                    // Add a new regular draft to the top
                    Timber.d("Draft: add a new draft ${privacySafe(draft)}")
                    sessionDatabase.draftQueries.insertDraft(roomId, draft)
                }
            } else {
                // Top draft is not an edit
                if (draft is UserDraft.EDIT) {
                    Timber.d("Draft: create a new edit draft ${privacySafe(draft)}")
                    sessionDatabase.draftQueries.insertDraft(roomId, draft)
                } else {
                    // Update the top draft
                    Timber.d("Draft: update the top draft ${privacySafe(draft)}")
                    sessionDatabase.draftQueries.updateTopDraft(roomId, draft)
                }
            }
        } else {
            // There is no draft to save, so the composer was clear
            Timber.d("Draft: delete a draft")
            sessionDatabase.draftQueries.deleteTopDraftFromRoom(roomId)
        }
    }

    fun getDraftsLive(roomId: String): Flow<List<UserDraft>> {
        return sessionDatabase.draftQueries.getAllFromRoom(roomId, draftMapper::map).asFlow().mapToList(coroutineDispatchers.dbQuery)
    }

    private fun DraftQueries.insertDraft(roomId: String, userDraft: UserDraft) {
        when (userDraft) {
            is UserDraft.REGULAR -> insert(room_id = roomId, content = userDraft.text, draft_mode = DraftMode.MODE_REGULAR, linked_event_id = "")
            is UserDraft.EDIT -> insert(room_id = roomId, content = userDraft.text, draft_mode = DraftMode.MODE_EDIT, linked_event_id = userDraft.linkedEventId)
            is UserDraft.QUOTE -> insert(room_id = roomId, content = userDraft.text, draft_mode = DraftMode.MODE_QUOTE, linked_event_id = userDraft.linkedEventId)
            is UserDraft.REPLY -> insert(room_id = roomId, content = userDraft.text, draft_mode = DraftMode.MODE_REPLY, linked_event_id = userDraft.linkedEventId)
        }
    }

    private fun DraftQueries.updateTopDraft(roomId: String, userDraft: UserDraft) {
        when (userDraft) {
            is UserDraft.REGULAR -> updateTopDraft(roomId = roomId, content = userDraft.text, draftMode = DraftMode.MODE_REGULAR, linkedEventId = "")
            is UserDraft.EDIT -> updateTopDraft(roomId = roomId, content = userDraft.text, draftMode = DraftMode.MODE_EDIT, linkedEventId = userDraft.linkedEventId)
            is UserDraft.QUOTE -> updateTopDraft(roomId = roomId, content = userDraft.text, draftMode = DraftMode.MODE_QUOTE, linkedEventId = userDraft.linkedEventId)
            is UserDraft.REPLY -> updateTopDraft(roomId = roomId, content = userDraft.text, draftMode = DraftMode.MODE_REPLY, linkedEventId = userDraft.linkedEventId)
        }
    }

    private fun privacySafe(o: Any): Any {
        if (BuildConfig.LOG_PRIVATE_DATA) {
            return o
        }
        return ""
    }
}
