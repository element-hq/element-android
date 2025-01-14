/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory.createroom

import android.net.Uri
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class CreateRoomViewState(
        val avatarUri: Uri? = null,
        val roomName: String = "",
        val roomTopic: String = "",
        val roomJoinRules: RoomJoinRules = RoomJoinRules.INVITE,
        val isEncrypted: Boolean? = null,
        val defaultEncrypted: Map<RoomJoinRules, Boolean> = emptyMap(),
        val showAdvanced: Boolean = false,
        val disableFederation: Boolean = false,
        val homeServerName: String = "",
        val hsAdminHasDisabledE2E: Boolean = false,
        val asyncCreateRoomRequest: Async<String> = Uninitialized,
        val parentSpaceId: String?,
        val parentSpaceSummary: RoomSummary? = null,
        val supportsRestricted: Boolean = false,
        val aliasLocalPart: String? = null,
        val isSubSpace: Boolean = false,
        val openAfterCreate: Boolean = true
) : MavericksState {

    constructor(args: CreateRoomArgs) : this(
            roomName = args.initialName,
            parentSpaceId = args.parentSpaceId,
            isSubSpace = args.isSpace,
            openAfterCreate = args.openAfterCreate
    )

    /**
     * Return true if there is not important input from user.
     */
    fun isEmpty() = avatarUri == null &&
            roomName.isEmpty() &&
            roomTopic.isEmpty() &&
            aliasLocalPart.isNullOrEmpty()
}
