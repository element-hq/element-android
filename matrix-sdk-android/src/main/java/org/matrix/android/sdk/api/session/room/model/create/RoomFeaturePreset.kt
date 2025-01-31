/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.create

import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.room.model.GuestAccess
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomJoinRulesAllowEntry
import org.matrix.android.sdk.api.session.room.model.RoomJoinRulesContent

interface RoomFeaturePreset {

    fun updateRoomParams(params: CreateRoomParams)

    fun setupInitialStates(): List<CreateRoomStateEvent>?
}

class RestrictedRoomPreset(val homeServerCapabilities: HomeServerCapabilities, val restrictedList: List<RoomJoinRulesAllowEntry>) : RoomFeaturePreset {

    override fun updateRoomParams(params: CreateRoomParams) {
        params.historyVisibility = params.historyVisibility ?: RoomHistoryVisibility.SHARED
        params.guestAccess = params.guestAccess ?: GuestAccess.Forbidden
        params.roomVersion = homeServerCapabilities.versionOverrideForFeature(HomeServerCapabilities.ROOM_CAP_RESTRICTED)
    }

    override fun setupInitialStates(): List<CreateRoomStateEvent> {
        return listOf(
                CreateRoomStateEvent(
                        type = EventType.STATE_ROOM_JOIN_RULES,
                        stateKey = "",
                        content = RoomJoinRulesContent(
                                joinRulesStr = RoomJoinRules.RESTRICTED.value,
                                allowList = restrictedList
                        ).toContent()
                )
        )
    }
}
