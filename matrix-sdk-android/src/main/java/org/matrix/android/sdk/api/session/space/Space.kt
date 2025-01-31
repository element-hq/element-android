/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.space

import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.space.model.SpaceChildContent

interface Space {

    fun asRoom(): Room

    val spaceId: String

    /**
     * A current snapshot of [RoomSummary] associated with the space.
     */
    fun spaceSummary(): RoomSummary?

    suspend fun addChildren(
            roomId: String,
            viaServers: List<String>?,
            order: String?,
//                            autoJoin: Boolean = false,
            suggested: Boolean? = false
    )

    fun getChildInfo(roomId: String): SpaceChildContent?

    suspend fun removeChildren(roomId: String)

    @Throws
    suspend fun setChildrenOrder(roomId: String, order: String?)

//    @Throws
//    suspend fun setChildrenAutoJoin(roomId: String, autoJoin: Boolean)

    @Throws
    suspend fun setChildrenSuggested(roomId: String, suggested: Boolean)

//    fun getChildren() : List<IRoomSummary>
}
