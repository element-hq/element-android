/*
 * Copyright 2020 New Vector Ltd
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
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.api.session.room.send.UserDraft
import org.matrix.android.sdk.internal.database.mapper.DraftMapper
import org.matrix.android.sdk.internal.database.model.DraftEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.UserDraftsEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.util.awaitTransaction
import io.realm.Realm
import io.realm.kotlin.createObject
import timber.log.Timber
import javax.inject.Inject

class DraftRepository @Inject constructor(@SessionDatabase private val monarchy: Monarchy) {

    suspend fun saveDraft(roomId: String, userDraft: UserDraft) {
        monarchy.awaitTransaction {
            saveDraft(it, userDraft, roomId)
        }
    }

    suspend fun deleteDraft(roomId: String) {
        monarchy.awaitTransaction {
            deleteDraft(it, roomId)
        }
    }

    private fun deleteDraft(realm: Realm, roomId: String) {
        UserDraftsEntity.where(realm, roomId).findFirst()?.let { userDraftsEntity ->
            if (userDraftsEntity.userDrafts.isNotEmpty()) {
                userDraftsEntity.userDrafts.removeAt(userDraftsEntity.userDrafts.size - 1)
            }
        }
    }

    private fun saveDraft(realm: Realm, draft: UserDraft, roomId: String) {
        val roomSummaryEntity = RoomSummaryEntity.where(realm, roomId).findFirst()
                ?: realm.createObject(roomId)

        val userDraftsEntity = roomSummaryEntity.userDrafts
                ?: realm.createObject<UserDraftsEntity>().also {
                    roomSummaryEntity.userDrafts = it
                }

        userDraftsEntity.let { userDraftEntity ->
            // Save only valid draft
            if (draft.isValid()) {
                // Add a new draft or update the current one?
                val newDraft = DraftMapper.map(draft)

                // Is it an update of the top draft?
                val topDraft = userDraftEntity.userDrafts.lastOrNull()

                if (topDraft == null) {
                    Timber.d("Draft: create a new draft ${privacySafe(draft)}")
                    userDraftEntity.userDrafts.add(newDraft)
                } else if (topDraft.draftMode == DraftEntity.MODE_EDIT) {
                    // top draft is an edit
                    if (newDraft.draftMode == DraftEntity.MODE_EDIT) {
                        if (topDraft.linkedEventId == newDraft.linkedEventId) {
                            // Update the top draft
                            Timber.d("Draft: update the top edit draft ${privacySafe(draft)}")
                            topDraft.content = newDraft.content
                        } else {
                            // Check a previously EDIT draft with the same id
                            val existingEditDraftOfSameEvent = userDraftEntity.userDrafts.find {
                                it.draftMode == DraftEntity.MODE_EDIT && it.linkedEventId == newDraft.linkedEventId
                            }

                            if (existingEditDraftOfSameEvent != null) {
                                // Ignore the new text, restore what was typed before, by putting the draft to the top
                                Timber.d("Draft: restore a previously edit draft ${privacySafe(draft)}")
                                userDraftEntity.userDrafts.remove(existingEditDraftOfSameEvent)
                                userDraftEntity.userDrafts.add(existingEditDraftOfSameEvent)
                            } else {
                                Timber.d("Draft: add a new edit draft ${privacySafe(draft)}")
                                userDraftEntity.userDrafts.add(newDraft)
                            }
                        }
                    } else {
                        // Add a new regular draft to the top
                        Timber.d("Draft: add a new draft ${privacySafe(draft)}")
                        userDraftEntity.userDrafts.add(newDraft)
                    }
                } else {
                    // Top draft is not an edit
                    if (newDraft.draftMode == DraftEntity.MODE_EDIT) {
                        Timber.d("Draft: create a new edit draft ${privacySafe(draft)}")
                        userDraftEntity.userDrafts.add(newDraft)
                    } else {
                        // Update the top draft
                        Timber.d("Draft: update the top draft ${privacySafe(draft)}")
                        topDraft.draftMode = newDraft.draftMode
                        topDraft.content = newDraft.content
                        topDraft.linkedEventId = newDraft.linkedEventId
                    }
                }
            } else {
                // There is no draft to save, so the composer was clear
                Timber.d("Draft: delete a draft")

                val topDraft = userDraftEntity.userDrafts.lastOrNull()

                if (topDraft == null) {
                    Timber.d("Draft: nothing to do")
                } else {
                    // Remove the top draft
                    Timber.d("Draft: remove the top draft")
                    userDraftEntity.userDrafts.remove(topDraft)
                }
            }
        }
    }

    fun getDraftsLive(roomId: String): LiveData<List<UserDraft>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { UserDraftsEntity.where(it, roomId) },
                {
                    it.userDrafts.map { draft ->
                        DraftMapper.map(draft)
                    }
                }
        )
        return Transformations.map(liveData) {
            it.firstOrNull().orEmpty()
        }
    }

    private fun privacySafe(o: Any): Any {
        if (BuildConfig.LOG_PRIVATE_DATA) {
            return o
        }
        return ""
    }
}
