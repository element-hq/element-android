/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.mapper

import com.squareup.moshi.Moshi
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataEvent
import org.matrix.android.sdk.api.session.room.accountdata.RoomAccountDataEvent
import org.matrix.android.sdk.api.util.JSON_DICT_PARAMETERIZED_TYPE
import org.matrix.android.sdk.internal.database.model.RoomAccountDataEntity
import org.matrix.android.sdk.internal.database.model.UserAccountDataEntity
import javax.inject.Inject

internal class AccountDataMapper @Inject constructor(moshi: Moshi) {

    private val adapter = moshi.adapter<Map<String, Any>>(JSON_DICT_PARAMETERIZED_TYPE)

    fun map(entity: UserAccountDataEntity): UserAccountDataEvent {
        return UserAccountDataEvent(
                type = entity.type ?: "",
                content = entity.contentStr?.let { adapter.fromJson(it) }.orEmpty()
        )
    }

    fun map(roomId: String, entity: RoomAccountDataEntity): RoomAccountDataEvent {
        return RoomAccountDataEvent(
                roomId = roomId,
                type = entity.type ?: "",
                content = entity.contentStr?.let { adapter.fromJson(it) }.orEmpty()
        )
    }
}
