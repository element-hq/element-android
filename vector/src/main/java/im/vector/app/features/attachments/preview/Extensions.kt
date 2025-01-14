/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.attachments.preview

import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.util.MimeTypes
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeImage

/**
 * All images are editable, expect Gif.
 */
fun ContentAttachmentData.isEditable(): Boolean {
    return type == ContentAttachmentData.Type.IMAGE &&
            getSafeMimeType()?.isMimeTypeImage() == true &&
            getSafeMimeType() != MimeTypes.Gif
}
