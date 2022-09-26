/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.matrix.android.sdk.api.session.room.model.LocalRoomCreationState
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.toJSONString

internal open class LocalRoomSummaryEntity(
        @PrimaryKey var roomId: String = "",
        var roomSummaryEntity: RoomSummaryEntity? = null,
        var replacementRoomId: String? = null,
) : RealmObject() {

    private var stateStr: String = LocalRoomCreationState.NOT_CREATED.name
    var creationState: LocalRoomCreationState
        get() = LocalRoomCreationState.valueOf(stateStr)
        set(value) {
            stateStr = value.name
        }

    private var createRoomParamsStr: String? = null
    var createRoomParams: CreateRoomParams?
        get() {
            return CreateRoomParams.fromJson(createRoomParamsStr)
        }
        set(value) {
            createRoomParamsStr = value?.toJSONString()
        }

    companion object
}
