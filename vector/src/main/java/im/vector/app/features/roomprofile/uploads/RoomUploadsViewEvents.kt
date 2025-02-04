/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.uploads

import im.vector.app.core.platform.VectorViewEvents
import java.io.File

sealed class RoomUploadsViewEvents : VectorViewEvents {
    data class Failure(val throwable: Throwable) : RoomUploadsViewEvents()

    data class FileReadyForSharing(val file: File) : RoomUploadsViewEvents()
    data class FileReadyForSaving(val file: File, val title: String) : RoomUploadsViewEvents()
}
