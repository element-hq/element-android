/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
