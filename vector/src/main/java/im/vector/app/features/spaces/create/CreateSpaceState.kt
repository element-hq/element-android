/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.create

import android.net.Uri
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized

data class CreateSpaceState(
        val name: String? = null,
        val avatarUri: Uri? = null,
        val topic: String = "",
        val step: Step = Step.ChooseType,
        val spaceType: SpaceType? = null,
        val spaceTopology: SpaceTopology? = null,
        val homeServerName: String = "",
        val aliasLocalPart: String? = null,
        val aliasManuallyModified: Boolean = false,
        val aliasVerificationTask: Async<Boolean> = Uninitialized,
        val nameInlineError: String? = null,
        val defaultRooms: Map<Int, String?>? = null, // Int: position in form
        val default3pidInvite: Map<Int, String?>? = null, // Int: position in form
        val emailValidationResult: Map<Int, Boolean>? = null, // Int: position in form
        val creationResult: Async<String> = Uninitialized,
        val canInviteByMail: Boolean = false
) : MavericksState {

    enum class Step {
        ChooseType,
        SetDetails,
        AddRooms,
        ChoosePrivateType,
        AddEmailsOrInvites
    }
}
