/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.attachments

import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.util.MimeTypes

private val listOfPreviewableMimeTypes = listOf(
        MimeTypes.Jpeg,
        MimeTypes.Png,
        MimeTypes.Gif
)

fun ContentAttachmentData.isPreviewable(): Boolean {
    // Preview supports image and video
    return (type == ContentAttachmentData.Type.IMAGE &&
            listOfPreviewableMimeTypes.contains(getSafeMimeType() ?: "")) ||
            type == ContentAttachmentData.Type.VIDEO
}

data class GroupedContentAttachmentData(
        val previewables: List<ContentAttachmentData>,
        val notPreviewables: List<ContentAttachmentData>
)

fun List<ContentAttachmentData>.toGroupedContentAttachmentData(): GroupedContentAttachmentData {
    return groupBy { it.isPreviewable() }
            .let {
                GroupedContentAttachmentData(
                        it[true].orEmpty(),
                        it[false].orEmpty()
                )
            }
}
