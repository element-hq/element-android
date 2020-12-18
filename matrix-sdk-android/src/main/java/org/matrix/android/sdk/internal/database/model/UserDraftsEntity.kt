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

package org.matrix.android.sdk.internal.database.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects

/**
 * Create a specific table to be able to do direct query on it and keep the draft ordered
 */
internal open class UserDraftsEntity(var userDrafts: RealmList<DraftEntity> = RealmList()
) : RealmObject() {

    // Link to RoomSummaryEntity
    @LinkingObjects("userDrafts")
    val roomSummaryEntity: RealmResults<RoomSummaryEntity>? = null

    companion object
}
