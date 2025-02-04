/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.media

import im.vector.app.core.platform.VectorViewEvents

sealed class VectorAttachmentViewerViewEvents : VectorViewEvents {
    data class ErrorDownloadingMedia(val error: Throwable) : VectorAttachmentViewerViewEvents()
}
