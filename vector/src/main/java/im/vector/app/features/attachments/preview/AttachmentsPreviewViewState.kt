/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.attachments.preview

import com.airbnb.mvrx.MavericksState
import org.matrix.android.sdk.api.session.content.ContentAttachmentData

data class AttachmentsPreviewViewState(
        val attachments: List<ContentAttachmentData>,
        val currentAttachmentIndex: Int = 0,
        val sendImagesWithOriginalSize: Boolean = false
) : MavericksState {

    constructor(args: AttachmentsPreviewArgs) : this(attachments = args.attachments)
}
