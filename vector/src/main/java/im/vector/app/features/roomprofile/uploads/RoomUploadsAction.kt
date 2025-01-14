/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.uploads

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.uploads.UploadEvent

sealed class RoomUploadsAction : VectorViewModelAction {
    data class Download(val uploadEvent: UploadEvent) : RoomUploadsAction()
    data class Share(val uploadEvent: UploadEvent) : RoomUploadsAction()

    object Retry : RoomUploadsAction()
    object LoadMore : RoomUploadsAction()
}
