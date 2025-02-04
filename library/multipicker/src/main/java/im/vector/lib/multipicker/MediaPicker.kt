/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker

import android.content.Context
import android.content.Intent
import im.vector.lib.multipicker.entity.MultiPickerBaseMediaType
import im.vector.lib.multipicker.utils.isMimeTypeVideo
import im.vector.lib.multipicker.utils.toMultiPickerImageType
import im.vector.lib.multipicker.utils.toMultiPickerVideoType

/**
 * Image/Video Picker implementation.
 */
class MediaPicker : Picker<MultiPickerBaseMediaType>() {

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * Returns selected image/video files or empty list if user did not select any files.
     */
    override fun getSelectedFiles(context: Context, data: Intent?): List<MultiPickerBaseMediaType> {
        return getSelectedUriList(context, data).mapNotNull { selectedUri ->
            val mimeType = context.contentResolver.getType(selectedUri)

            if (mimeType.isMimeTypeVideo()) {
                selectedUri.toMultiPickerVideoType(context)
            } else {
                // Assume it's an image
                selectedUri.toMultiPickerImageType(context)
            }
        }
    }

    override fun createIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !single)
            type = "*/*"
            val mimeTypes = arrayOf("image/*", "video/*")
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
    }
}
