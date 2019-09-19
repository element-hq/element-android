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

package im.vector.matrix.android.internal.session.room.draft

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.BuildConfig
import im.vector.matrix.android.api.session.room.send.DraftService
import im.vector.matrix.android.api.session.room.send.UserDraft
import im.vector.matrix.android.internal.database.RealmLiveData
import im.vector.matrix.android.internal.database.mapper.DraftMapper
import im.vector.matrix.android.internal.database.model.DraftEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.UserDraftsEntity
import im.vector.matrix.android.internal.database.query.where
import io.realm.kotlin.createObject
import timber.log.Timber

internal class DefaultDraftService @AssistedInject constructor(@Assisted private val roomId: String,
                                                               private val monarchy: Monarchy
) : DraftService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): DraftService
    }

    /**
     * The draft stack can contain several drafts. Depending of the draft to save, it will update the top draft, or create a new draft,
     * or even move an existing draft to the top of the list
     */
    override fun saveDraft(draft: UserDraft) {
        Timber.d("Draft: saveDraft ${privacySafe(draft)}")

        monarchy.writeAsync { realm ->

            val roomSummaryEntity = RoomSummaryEntity.where(realm, roomId).findFirst() ?: realm.createObject(roomId)

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
    }

    private fun privacySafe(o: Any): Any {
        if (BuildConfig.LOG_PRIVATE_DATA) {
            return o
        }

        return ""
    }

    override fun deleteDraft() {
        Timber.d("Draft: deleteDraft()")

        monarchy.writeAsync { realm ->
            UserDraftsEntity.where(realm, roomId).findFirst()?.let { userDraftsEntity ->
                if (userDraftsEntity.userDrafts.isNotEmpty()) {
                    userDraftsEntity.userDrafts.removeAt(userDraftsEntity.userDrafts.size - 1)
                }
            }
        }
    }

    override fun getDraftsLive(): LiveData<List<UserDraft>> {
        val liveData = RealmLiveData(monarchy.realmConfiguration) {
            UserDraftsEntity.where(it, roomId)
        }

        return Transformations.map(liveData) { userDraftsEntities ->
            userDraftsEntities.firstOrNull()?.let { userDraftEntity ->
                userDraftEntity.userDrafts.map { draftEntity ->
                    DraftMapper.map(draftEntity)
                }
            } ?: emptyList()
        }
    }
}

