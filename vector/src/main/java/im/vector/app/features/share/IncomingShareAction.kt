/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.share

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.model.RoomSummary

sealed class IncomingShareAction : VectorViewModelAction {
    data class SelectRoom(val roomSummary: RoomSummary, val enableMultiSelect: Boolean) : IncomingShareAction()
    object ShareToSelectedRooms : IncomingShareAction()
    data class ShareToRoom(val roomId: String) : IncomingShareAction()
    data class ShareMedia(val keepOriginalSize: Boolean) : IncomingShareAction()
    data class FilterWith(val filter: String) : IncomingShareAction()
    data class UpdateSharedData(val sharedData: SharedData) : IncomingShareAction()
}
