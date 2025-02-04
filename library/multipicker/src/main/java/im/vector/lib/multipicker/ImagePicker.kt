/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker

import android.content.Context
import android.content.Intent
import im.vector.lib.multipicker.entity.MultiPickerImageType
import im.vector.lib.multipicker.utils.toMultiPickerImageType

/**
 * Image Picker implementation.
 */
class ImagePicker : Picker<MultiPickerImageType>() {

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * Returns selected image files or empty list if user did not select any files.
     */
    override fun getSelectedFiles(context: Context, data: Intent?): List<MultiPickerImageType> {
        return getSelectedUriList(context, data).mapNotNull { selectedUri ->
            selectedUri.toMultiPickerImageType(context)
        }
    }

    override fun createIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !single)
            type = "image/*"
        }
    }
}
